/* TURN ON GPS: AT+CGNSPWR=1
   GPS GET AT COMMAND: AT+CGNSINF
 * SIM SEND SMS: AT+CMGS="number" <CR> [message] <CTRL-Z>

    HTTP GET SEQUENCE
        AT+SAPBR=3,1,"CONTYPE","GPRS"

        AT+SAPBR=3,1,"APN","wholesale"
        The remaining list of commands:
        AT+CGATT=1

        AT+SAPBR=1,1 (confirm you have an IP address with `AT+SAPBR=2,1`)

        AT+HTTPINIT

        AT+HTTPPARA="CID",1

        AT+HTTPSSL=1 (optional if you need `https`)

        AT+HTTPPARA="URL","<url>"

        AT+HTTPPARA="CONTENT","application/json"

        AT+HTTPDATA=<strlen>,<data_timeout> (wait for the DOWNLOAD response)

        <json_data>

        AT+HTTPACTION=1

        AT+HTTPREAD (allow time for the server to respond before executing)

        AT+HTTPTERM

        AT+CGATT=0
*/

#include <SoftwareSerial.h>

int RX_SS = 11;
int TX_SS = 10;


int L_RED = 2;
int L_YEL = 3;
int L_GRE = 4;


SoftwareSerial sim_808(RX_SS, TX_SS);

void setup()
{
    setup_leds();
    startup_timer(16);
    setup_serials();
 
    // Turn on GPS
    write_at_command("at+cgnspwr=1");

    // Setup GSM context
    write_at_command("at+sapbr=3,1,\"CONTYPE\",\"GPRS\"");
    write_at_command("at+sapbr=3,1,\"APN\",\"wholesale\"");
    write_at_command("at+sapbr=1,1");
    write_at_command("at+sapbr=2,1");
    write_at_command("at+httpinit");
   
    // Flash LED's to signal setup completion
    //signal_finish_setup();    
     
}


void loop()
{
    // Send GPS at command    
    
    char gps_in[1024];
    for (int y=0; y < 1024; ++y) {
        gps_in[y] = '\0';
    }
    strcpy(gps_in, at_url_param);
    debug("this is url string");
    debug(gps_in);
    sim_808.write("at+cgnsinf\n");
    delay(1000);
    

    if (sim_808.available()) {
        int x = strlen(gps_in)-1;
        debug("String length is " + String(x));
        while (sim_808.available()) {
            unsigned char c = sim_808.read();
            if ((c != '\n') && (!isspace(c))) {
                gps_in[x++] = c;
                flash_pin(L_YEL);
            }
        }
        gps_in[x++] = '\"';
        gps_in[x] = '\n';
    }
    debug("this is url string after");
    debug(gps_in);
    int byte_counter = 0;
    for (int y=0; (y < 1024) && (gps_in[y] != '\0'); ++y) {
        sim_808.write(gps_in[y]);
        byte_counter = y;
    }
    sim_808.write("\n");
    delay(3000);
    
    signal_finish_setup();   
    read_sim808();
    debug("This is how many bytes were actually written: " + String(byte_counter));
    debug("This is how many bytes are in gps in: " + String(strlen(gps_in)));
    signal_finish_setup();   
    write_at_command("at+httppara=\"CID\",1");
    delay(1000);
    write_at_command("at+httpaction=0");



    // Wait for server response, then read reply
    delay(3000);
    write_at_command("at+httpread");

    // TODO: Add logic to check for HTTP status 
    debug("Successfully deployed request");

    // Terminate HTTP context
    write_at_command("at+httpterm");
    delay(5000);
    
}


void debug(String log) {
    Serial.println(log);
}


void setup_leds() {

    pinMode(L_RED, OUTPUT);
    pinMode(L_YEL, OUTPUT);
    pinMode(L_GRE, OUTPUT);
}


void startup_timer(int seconds) { 

    digitalWrite(L_RED, HIGH);
    delay((seconds*1000)/2);
    
    digitalWrite(L_RED, LOW);
    digitalWrite(L_YEL, HIGH);
    delay((seconds*1000)/2);

    flash_pin(L_YEL);
    digitalWrite(L_GRE, HIGH);
}


void setup_serials() {

    Serial.begin(9600);
    sim_808.begin(9600);

}


void read_sim808() {

    while (sim_808.available()) {
        Serial.write(sim_808.read());
        flash_pin(L_YEL);
    }
}


void write_at_command(String at_command) {
    
    delay(500);
    flash_pin(L_RED);
    at_command.concat("\n");
    sim_808.write(at_command.c_str());
    delay(500);
    read_sim808();
}
void write_at_command_c(char * at_command) {
    
    delay(500);
    flash_pin(L_RED);
    sim_808.write(at_command);
    delay(1500);
    sim_808.write("\n");

}


void signal_finish_setup() {

    for (int f = 0; f < 10; f++) {
        flash_pin(L_RED);
        delay(100);
        flash_pin(L_YEL);
        delay(100);
        flash_pin(L_GRE);
        delay(100);
    }
}

void flash_pin(int pin) {
    for (int x = 0; x < 1; ++x) {
        digitalWrite(pin, HIGH);
        delay(25);
        digitalWrite(pin, LOW);
        delay(25);
    }
}


