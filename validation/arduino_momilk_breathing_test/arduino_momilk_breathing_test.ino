//#include <AltSoftSerial.h>
//AltSoftSerial swSerial;

#include <SoftwareSerial.h>// import the serial library
SoftwareSerial swSerial(10, 11); // RX, TX 

static const int NUM_OF_PACKETS_TO_SEND = 100;
static const int ASK_FOR_STATUS_AFTER_EACH_N_PACKETS = 10;

static const int INPUT_BUFFER_LENGTH = 100;
static const int MESSAGE_BUFFER_LENGTH = 150;
static const int MAX_NUM_OF_INPUT_TOKENS = 15;
static const int MAX_TOKEN_LENGTH = 10;


static const int STATE_IDLE = 0;
static const int STATE_WAIT_FOR_SESSION_START = 1;
static const int STATE_SEND_PACKETS = 2;
static const int STATE_ASK_FOR_STATUS = 3;
static const int STATE_WAIT_FOR_STATUS = 4;
static const int STATE_SEND_PACKETS_COUNT = 5;

// These variables should be global because their state must be 
// preserved over functions' calls 
static int sCurrState;
static int sPacketsSent;
static int sLastStatusUpdateAtPackets;

// This function takes in a string and splits it into tokens according to DELIMETER value
char** myTokenize (char * string);

// This function frees the memory reserved by myTokenize function (it should be provided 
// with the original pointer returned by myTokenize)
void freeTokenizeMemory(char** tokenizePtr);



void setup() {
  Serial.begin(9600);     // Serial serial begin 9600 baud rate
  swSerial.begin(9600);   // Blt serial begin 9600 baud rate

  sCurrState = STATE_IDLE;
  sPacketsSent = 0;
  sLastStatusUpdateAtPackets = 0;
}


void loop(){

  char commandBuffer[INPUT_BUFFER_LENGTH];
  char stringBuffer[MESSAGE_BUFFER_LENGTH];

  int i;
  char nextChar;
  long randNum;
  char** tokens = NULL;
  boolean isMsgConsumed = false;

  // ----------------------------------------------------------------------------------------------
  //
  // Send input from Serial over bluetooth and continue to the next iteration of the loop
  //
  // ----------------------------------------------------------------------------------------------

  if (Serial.available()){
    delay(100);
    i = 0;
    while( Serial.available() && (i < 99)) {
      commandBuffer[i++] = Serial.read();
    }
    commandBuffer[i++]='\0';

    if(i>0) {
      swSerial.write((char*)commandBuffer);
    }

    return; // This return for arduino is the equivalent of regular "continue"  
  }


  // ----------------------------------------------------------------------------------------------
  //
  // Get message from bluetooth and parse it into tokens (will be used in state specific logic)
  //
  // ----------------------------------------------------------------------------------------------

  if(swSerial.available()){
    delay(100);
    i = 0;
    // Get the contents of the packet
    while( swSerial.available() && (i < 99)) {
      nextChar = swSerial.read();
      if (nextChar != '\n') {
        commandBuffer[i++] = nextChar;
      } 
      else {
        break; 
      }
    }
    commandBuffer[i++]='\0';

    if(i > 1) {
      // Print the original message
      stringBuffer[0] = '\0';
      sprintf(stringBuffer, "Got a message: %s\n", commandBuffer);
      Serial.write(stringBuffer);

      tokens = myTokenize(commandBuffer);

      // Print the tokens (for debug purpose)
      for(i = 0; tokens[i] != NULL; i++)
      {
        stringBuffer[0] = '\0';
        sprintf(stringBuffer, "The token at index %d is %s\n", i, tokens[i]);
        Serial.write(stringBuffer);
      }
    }
  } 


  // ----------------------------------------------------------------------------------------------
  //
  // State specific logic
  //
  // ----------------------------------------------------------------------------------------------

  if (sCurrState == STATE_IDLE) {
    // Consume "time update" packet
    if (tokens != NULL && *tokens[0] == 'T') {
      isMsgConsumed = true;
      sCurrState = STATE_WAIT_FOR_SESSION_START;
      Serial.write("Current state: STATE_WAIT_FOR_SESSION_START\n");
    } 
  }
  else if (sCurrState == STATE_WAIT_FOR_SESSION_START) {
    // Consume "start breathing session" packet
    if (tokens != NULL && *tokens[0] == 'B') {
      isMsgConsumed = true;
      sPacketsSent = 0;
      sLastStatusUpdateAtPackets = 0;
      sCurrState = STATE_SEND_PACKETS;
      Serial.write("Current state: STATE_SEND_PACKETS\n");
    }
  }
  else if (sCurrState == STATE_SEND_PACKETS) {
    if ((sPacketsSent % ASK_FOR_STATUS_AFTER_EACH_N_PACKETS == 0) && sLastStatusUpdateAtPackets < sPacketsSent) {
      // Ask for a status each ASK_FOR_STATUS_AFTER_EACH_N_PACKETS packets
      sLastStatusUpdateAtPackets = sPacketsSent;
      sCurrState = STATE_ASK_FOR_STATUS;
      Serial.write("Current state: STATE_ASK_FOR_STATUS\n");
    }   
    else if (sPacketsSent < NUM_OF_PACKETS_TO_SEND) {   
      randNum = random(1,10000);
      // Create the packet
      stringBuffer[0] = '\0';
      sprintf(stringBuffer, "%i@%lu@%lu@%lu\n", sPacketsSent, randNum, randNum, randNum);
      // Print the packet for debug
      Serial.write("Sending: ");
      Serial.write(stringBuffer);
      // Send the packet
      swSerial.write(stringBuffer);
      sPacketsSent++;
      delay(20);
    }
    else {
      // If all packets have already been sent - keep asking for a status until finished
      sLastStatusUpdateAtPackets = sPacketsSent;
      sCurrState = STATE_ASK_FOR_STATUS;
      Serial.write("Current state: STATE_ASK_FOR_STATUS\n");      
    }
  }
  else if (sCurrState == STATE_ASK_FOR_STATUS) {
    // Create the packet
    stringBuffer[0] = '\0';
    sprintf(stringBuffer, "exit?\n");
    // Print the packet for debug
    Serial.write("Sending: ");
    Serial.write(stringBuffer);
    // Send the packet
    swSerial.write(stringBuffer);
    sCurrState = STATE_WAIT_FOR_STATUS;
    Serial.write("Current state: STATE_WAIT_FOR_STATUS\n");    
  }
  else if (sCurrState == STATE_WAIT_FOR_STATUS) {
    // Consume status update packet
    if (tokens != NULL) {
      if (*tokens[0] == 'y') {
        // Yes, the session was finished
        isMsgConsumed = true;
        sCurrState = STATE_SEND_PACKETS_COUNT;
        Serial.write("Current state: STATE_SEND_PACKETS_COUNT\n");
      }
      else if (*tokens[0] == 'n') {
        // No, the session is not finished yet
        isMsgConsumed = true;
        if (sPacketsSent < NUM_OF_PACKETS_TO_SEND) {
          sCurrState = STATE_SEND_PACKETS;
        } 
        else {
          // Just keep asking if the session is finished after all the packets have been sent
          sCurrState = STATE_ASK_FOR_STATUS;
        }
        Serial.write("Current state: STATE_SEND_PACKETS\n");
      }
    }
  }
  else if (sCurrState == STATE_SEND_PACKETS_COUNT) {
    // Create the packet
    stringBuffer[0] = '\0';
    sprintf(stringBuffer, "Z@%i\n", sPacketsSent);
    // Output for debug
    Serial.write("Sending: ");
    Serial.write(stringBuffer);
    swSerial.write(stringBuffer);
    sCurrState = STATE_IDLE;
    Serial.write("Current state: STATE_IDLE (protocol completed)\n");

  }

  // ----------------------------------------------------------------------------------------------
  //
  // Free memory allocated by myTokenize (if necessary) and handle unconsumed message (if necessary)
  //
  // ----------------------------------------------------------------------------------------------

  if (tokens != NULL) {

    if (!isMsgConsumed) {
      if (*tokens[0] == 'E') {
        Serial.write("Android app signaled timeout!\n");
        sPacketsSent = 0;
        sCurrState = STATE_IDLE;
        Serial.write("Current state: STATE_IDLE\n");
      }
      else {
        stringBuffer[0] = '\0';
        sprintf(stringBuffer, "ERROR: received unexpected message!\nState index: %d\nMessage: %s\n", sCurrState, commandBuffer);
        Serial.write(stringBuffer);
      } 
    }

    freeTokenizeMemory(tokens); 
  }
} // loop() 


//////////////////////////////////////////////////////////////////////////////////////////
//
// My functions
//
/////////////////////////////////////////////////////////////////////////////////////////

char** myTokenize (char * string) {

  int i;

  // Allocate copy of input argument
  char *copy = strdup(string);


  // Allocate 2D array of chars to hold tokens of the message
  char **tokens = (char**)malloc(MAX_NUM_OF_INPUT_TOKENS * sizeof(char *));
  for (i = 0; i < MAX_NUM_OF_INPUT_TOKENS; ++i) {
    tokens[i] = (char *)malloc(MAX_TOKEN_LENGTH+1);
  }

  char * token_ptr;

  // Parse the original message and store tokens in the array
  token_ptr = strtok (copy, "@");

  i = 0;
  while (token_ptr != NULL)
  {
    strcpy(tokens[i], token_ptr);
    token_ptr = strtok (NULL, "@");
    i++;
  }
  tokens[i] = NULL; // Array termination symbol

  free(copy);

  return tokens;
}


void freeTokenizeMemory(char** tokenizePtr) {      
  // Free all allocations
  for (int i = 0; i < MAX_NUM_OF_INPUT_TOKENS; ++i) {
    free(tokenizePtr[i]);
  }
  free(tokenizePtr); 
}














