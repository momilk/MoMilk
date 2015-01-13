#include <AltSoftSerial.h>
AltSoftSerial swSerial;

//#include <SoftwareSerial.h>// import the serial library
//SoftwareSerial swSerial(10, 11); // RX, TX 

static const int NUM_OF_PACKETS_TO_SEND = 100;

static const int INPUT_BUFFER_LENGTH = 100;
static const int MESSAGE_BUFFER_LENGTH = 150;
static const int MAX_NUM_OF_INPUT_TOKENS = 15;
static const int MAX_TOKEN_LENGTH = 10;


static const int STATE_IDLE = 0;
static const int STATE_WAIT_FOR_SESSION_START = 1;
static const int STATE_SEND_PACKETS = 2;
static const int STATE_SEND_PACKETS_COUNT = 3;

// These variables should be global because their state must be 
// preserved over functions' calls 
int currState;
int packetsSent;

// This function takes in a string and splits it into tokens according to DELIMETER value
char** myTokenize (char * string);

// This function frees the memory reserved by myTokenize function (it should be provided 
// with the original pointer returned by myTokenize)
void freeTokenizeMemory(char** tokenizePtr);

void setup() {
  Serial.begin(9600);     // Serial serial begin 9600 baud rate
  swSerial.begin(9600);   // Blt serial begin 9600 baud rate

  currState = STATE_IDLE;
  packetsSent = 0;
}

void loop (){

  char commandBuffer[INPUT_BUFFER_LENGTH];
  char stringBuffer[MESSAGE_BUFFER_LENGTH];

  int i;
  char nextChar;
  long randNum;

  if(swSerial.available()){
    delay(100);
    i = 0;
    // Get the contents of the packet
    while( swSerial.available() && (i < 99)) {
      nextChar = swSerial.read();
      if (nextChar != '\n') {
        commandBuffer[i++] = nextChar;
      } else {
        break; 
      }
    }
    commandBuffer[i++]='\0';

    if(i > 0) {
      
      // Print the original message
      stringBuffer[0] = '\0';
      sprintf(stringBuffer, "Got a message: %s\n", commandBuffer);
      Serial.write(stringBuffer);

      if (currState != STATE_IDLE && currState != STATE_WAIT_FOR_SESSION_START && currState != STATE_SEND_PACKETS) {
        Serial.write("ERROR: received input message in incorrect state!\n");  
        return;
      }

      char** tokens = myTokenize(commandBuffer);


      // Print the tokens (for debug purpose)
      for(i = 0; tokens[i] != NULL; i++)
      {
        stringBuffer[0] = '\0';
        sprintf(stringBuffer, "The token at index %d is %s\n", i, tokens[i]);
        Serial.write(stringBuffer);
      }

      if (*tokens[0] == 'T') {
        if (currState == STATE_IDLE) {
          packetsSent = 0;
          currState = STATE_WAIT_FOR_SESSION_START;
          Serial.write("Current state: STATE_WAIT_FOR_SESSION_START\n");
        } 
        else {
          Serial.write("ERROR: received 'T' message while not in STATE_IDLE!\n");  
        }
      } 
      else if (*tokens[0] == 'B') {
        if (currState == STATE_WAIT_FOR_SESSION_START) {
          currState = STATE_SEND_PACKETS;
          Serial.write("Current state: STATE_SEND_PACKETS\n");
        } 
        else if (currState == STATE_SEND_PACKETS) {
          currState = STATE_SEND_PACKETS_COUNT;
          Serial.write("Current state: STATE_SEND_PACKETS_COUNT\n");
        }
        else {
          Serial.write("ERROR: received 'B' message while not in STATE_WAIT_FOR_SESSION_START or STATE _SEND_PACKETS\n");  
        }
      }
      else {
        stringBuffer[0] = '\0';
        sprintf(stringBuffer, "ERROR: received unexpected message!\nState index: %d\nMessage: %s\n",
          currState, commandBuffer);
        Serial.write(stringBuffer);
      }
      freeTokenizeMemory(tokens);
    }
  } 
  else if (Serial.available()){
    delay(100);
    i = 0;
    while( Serial.available() && (i < 99)) {
      commandBuffer[i++] = Serial.read();
    }
    commandBuffer[i++]='\0';

    if(i>0) {
      swSerial.write((char*)commandBuffer);
    }
  }
  else if (currState == STATE_SEND_PACKETS) {   
    if (packetsSent < NUM_OF_PACKETS_TO_SEND) {   
      randNum = random(1,10000);
      // Create the packet
      stringBuffer[0] = '\0';
      sprintf(stringBuffer, "%i@%lu@%lu@%lu\n", packetsSent, randNum, randNum, randNum);
      // Print the packet for debug
      Serial.write(stringBuffer);
      // Send the packet
      swSerial.write(stringBuffer);
      packetsSent++;
      delay(20);
   }
  }
  else if (currState == STATE_SEND_PACKETS_COUNT) {
    // TODO 
    // Create the packet
    stringBuffer[0] = '\0';
    sprintf(stringBuffer, "Z@%i\n", packetsSent);
    // Output for debug
    Serial.write(stringBuffer);
    swSerial.write(stringBuffer);
    currState = STATE_IDLE;
    packetsSent = 0;
    Serial.write("Current state: STATE_IDLE (protocol completed)\n");
    
  }
}


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




