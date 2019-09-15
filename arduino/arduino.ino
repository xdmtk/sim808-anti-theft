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
    pinMode(9, OUTPUT);
    setup_serials();
    setup_leds();
    delay(1500);
    digitalWrite(9, HIGH);
    delay(4000);
    delay(1500);
    startup_timer(16);
 
    // Turn on GPS
    write_at_command("at+cgnspwr=1");

    // Setup GSM context
    write_at_command("at+sapbr=3,1,\"CONTYPE\",\"GPRS\"");
    write_at_command("at+sapbr=3,1,\"APN\",\"wholesale\"");
    write_at_command("at+sapbr=1,1");
    write_at_command("at+sapbr=2,1");
   
    // Flash LED's to signal setup completion
    signal_finish_setup();    
     
}


void loop()
{
    char gps_in[1024];
    for (int y=0; y < 1024; ++y) {
        gps_in[y] = '\0';
    }
    strcpy(gps_in, at_url_param);

    char * at_init = "at+httpinit\n";
    char * at_gps = "at+cgnsinf\n";
    char * at_cid = "at+httppara=\"CID\",1\n";
    char * at_action = "at+httpaction=0\n";
    char * at_read = "at+httpread\n";
    char * at_term = "at+httpterm\n";

    // Send HTTP INIT
    sim_808.write(at_init);
    delay(1500);
    read_sim808();

    // Send GPS instruction
    sim_808.write(at_gps);
    delay(1500);
   
    // Read GPS response 
    if (sim_808.available()) {

        // Start entering response after data parameter in URL
        int x = strlen(gps_in)-1;
        while (sim_808.available()) {
            unsigned char c = sim_808.read();
            if ((c != '\n') && (!isspace(c))) {
                gps_in[x++] = c;
                flash_pin(L_YEL);
                Serial.write(c);
            }
        }
        // Close the at string and flush command
        gps_in[x++] = '\"';
        gps_in[x] = '\n';
        Serial.write("\n");
    }
   
    // Send CID parameter
    sim_808.write(at_cid);
    delay(1500);
    read_sim808();
    
    // Send URL parametr
    for (int y=0; y < 1024; ++y) {
        if (gps_in[y] == '\n') {
            break;
        }
        sim_808.write(gps_in[y]);
    }
    sim_808.write("\n");
  
    // Read response from URL parameter
    delay(4500);
    read_sim808();

    // Send ACTION Parameter 
    delay(6000);
    sim_808.write(at_action);
    delay(1500);
    read_sim808();

    // Wait for server response, then read reply
    delay(3000);
    sim_808.write(at_read);
    delay(4500);
    read_sim808();


    delay(3000);
    // Terminate HTTP context
    sim_808.write(at_term);
    delay(1500);
    read_sim808();
    delay(5000);
    
}


void debug(String log) {
    delay(500);
    Serial.println(log);
    delay(500);
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
    delay(1500);
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


