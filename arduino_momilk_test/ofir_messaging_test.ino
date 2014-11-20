

//device app symbol messages
String startSync = "S";
String endSync = "Z";
String appRespond ="R";
String endLine="\n";


#define blt_serial_in 11 //D11
#define blt_serial_out 10 //D10

#include <SoftwareSerial.h>

char lastread; //read byte from blt serial com
char* AppAck;  //holds answer of number of frames recivied in app

void setup(){ 
  BltSerial.begin(9600);
}

void loop(){
  //sends "S\n"
  startSync+=endLine;
  BltSerial.print(startSync);  
 
  //send measurement message
  ackData=sendDataToApp(frameIndex); //send measurment message
  if (ackData==true) // all frames sent to app: W@packet index.......................
  {
    //send end message to App = Z" + "@" + frame sent to app + "\n"
    endSync+="@";
    endSync+=frameIndex; //endSync hold  "
    endSync+=endLine;
    BltSerial.print(endSync); //send end symbol to measurement message
    /*
      wait To R respond from App => "R + @ + frames accepted App side +\n"
      exit while if timed out - not mentioned in code!!
    */
    
    while(BltSerial.available()>0 && 'R' == char(BltSerial.read()))
    {
      //read next char: should be "@"
      lastread = char(BltSerial.read());
      if (lastread=='@')
      {
        //read next byte after @: sholud be "frames from App"
        AppAck+=char(BltSerial.read());
	appRespond=true;
	break;
      }
      //didnt got answer from exit
      else{
          appRespond=false;
	  break;
      }
    } 
    //end while if accepted answer from App or timed out
    if (appRespond==true)
    {
	int packetFromApp=atoi(AppAck);
        if(packetFromApp==frameIndex){
             ///do something clear EEPROM etc..... 
        }
    }
  }
}







/*
  reads frames from EEPROM, if see 'E' value  end
  of frame, send DataToApp String and continue to next frame.
*/ 
boolean sendDataToApp(int frameIndex){
	String Endl = "\n";
	SoftwareSerial BltSerial(blt_serial_in,blt_serial_out); // RX, TX  MCU side
	BltSerial.begin(9600);// start blt comm.
	int address=0; //always start reading Sfrom start.
	//String lastRead; // holds last read byte from EEPROM and compare to end of frame char 'E'
	char lastRead;
	for ( int i = 0; i < frameIndex; i++){
		String DataToApp; //for each frame create a new String
		//BLT led blinks while transmission happens
		digitalWrite(LED_BLT,HIGH);
		delay(200);
		digitalWrite(LED_BLT,LOW);
		delay(200);
	//	int StrIndex=0;   //index to String Bytes, always start from index zero
		do 
		{
			if(address==512){ //out of bound error
				return false; 
			}
			lastRead=char(EEPROM.read(address)); 		
			//lastRead=String(EEPROM.read(address));
			if (lastRead!='E' )//&& lastRead!='a')
			{
				DataToApp+=String(lastRead);//read byte to str.	
						
				//BltSerial.print(DataToApp); 
		 	    //BltSerial.println(lastRead); //send each byte to App
			}
			address++;
	//		StrIndex++;
		}while (lastRead!='E');
		DataToApp+=Endl;
	    BltSerial.println(DataToApp); //transmit frame to App before reading next frame, TBD check why full string not sent to App
	}
	return true; // all frames has sent
 }
