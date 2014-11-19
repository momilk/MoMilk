#include <SoftwareSerial.h>// import the serial library

SoftwareSerial swSerial(10, 11); // RX, TX 


static const int INPUT_BUFFER_LENGTH = 100;
static const int MESSAGE_BUFFER_LENGTH = 150;
static const int MAX_NUM_OF_INPUT_TOKENS = 15;
static const int MAX_TOKEN_LENGTH = 10;


static const int STATE_IDLE = 0;
static const int STATE_SEND_PACKETS = 1;
static const int STATE_SEND_PACKETS_COUNT = 2;
static const int STATE_WAIT_FOR_ACK = 3;
static const int STATE_RECEIVED_CORRECT_ACK = 4;
static const int STATE_RECEIVED_INCORRECT_ACK = 5;

const char * const packets[] = {
  "W@1@L@12@00@00@18@11@2014@1@2@3@4\n",
  "W@2@R@1@0@0@22@10@2014@1@2@3@4\n",
  "W@3@R@02@02@10@19@11@2014@1@2@3@4\n",
  NULL };

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

  if(swSerial.available()){
    delay(100);
    i = 0;
    // Get the contents of the packet
    while( swSerial.available() && (i < 99)) {
      commandBuffer[i++] = swSerial.read();
    }
    commandBuffer[i++]='\0';

    if(i > 0) {

      if (currState != STATE_IDLE && currState != STATE_WAIT_FOR_ACK) {
        Serial.write("ERROR: received input message in incorrect state!\n");  
        return;
      }

      // Print the original message
      stringBuffer[0] = '\0';
      sprintf(stringBuffer, "Got a message: %s\n", commandBuffer);
      Serial.write(stringBuffer);


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
          currState = STATE_SEND_PACKETS;
          Serial.write("Current state: STATE_SEND_PACKETS\n");
        } 
        else {
          Serial.write("ERROR: received 'T' message while not in STATE_IDLE!\n");  
        }
      }
      else if (*tokens[0] == 'R') {
        if (currState == STATE_WAIT_FOR_ACK) {
          int packetsReceivedByAndroid = atoi(tokens[1]);
          if (packetsReceivedByAndroid == packetsSent) {
            currState = STATE_RECEIVED_CORRECT_ACK;
            Serial.write("Current state: STATE_RECEIVED_CORRECT_ACK\n");
          } 
          else {
            stringBuffer[0] = '\0';
            sprintf(stringBuffer, "Number of packets sent: %d; Number of packets acknowledged: %d\n", 
            packetsSent, packetsReceivedByAndroid);
            Serial.write(stringBuffer);
            currState = STATE_RECEIVED_INCORRECT_ACK;
            Serial.write("Current state: STATE_RECEIVED_INCORRECT_ACK\n");
          }
        } 
        else {
          Serial.write("ERROR: received 'R' message while not in STATE_WAIT_FOR_ACK!\n");  
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
  else if(Serial.available()){
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
    Serial.write("Sending 'Start Session' packet\n");
    // Send the "start session" packet
    swSerial.write("S\n");
    delay(100);


    Serial.write("Sending 'Data' packets\n");
    i = 0;
    while (packets[i] != NULL) {
      swSerial.write(packets[i]);
      i++;
      delay(100);
    }


    Serial.write("Sending 'Total Sent' packet\n");
    // Send the "total sent" packet
    stringBuffer[0] = '\0';
    sprintf(stringBuffer, "Z@%d\n", i);
    swSerial.write(stringBuffer);    

    packetsSent = i;
    currState = STATE_WAIT_FOR_ACK;
    Serial.write("Current state: STATE_WAIT_FOR_ACK\n");

  }
  else if (currState == STATE_RECEIVED_CORRECT_ACK) {
    currState = STATE_IDLE; 
    Serial.write("Current state: STATE_IDLE\n");
  }
  else if (currState == STATE_RECEIVED_INCORRECT_ACK) {
    currState = STATE_SEND_PACKETS; 
    Serial.write("Current state: STATE_SEND_PACKETS\n");
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




