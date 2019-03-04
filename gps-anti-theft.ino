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
    startup_timer(10);
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
    signal_finish_setup();    
     
}


void loop()
{
    debug("In loop");

    String gps_in = "";

    
    sim_808.write("at+cgnsinf\n");
    debug("issued GPS command");
    
    int x = 0;
    while (sim_808.available()) {
        gps_in.concat(sim_808.read());
        flash_pin(L_YEL);
    }
    debug("Got GPS string: " + gps_in);
    
    String https_request = form_request(gps_in);

    if (https_request.equals("ERROR")) {
        debug("Couldn't get GPS coordinates, probably not warmed up yet");
        for (int r = 0; r < 20; r++) {
            flash_pin(L_RED);
        }
        delay(2000);
        return;
    }
    else {
        debug("Sending request to: " + https_request);
    }



    write_at_command("at+httppara=\"CID\",1");
    write_at_command("at+httpssl=1");
    write_at_command("httppara=\"URL\",\"" + https_request + "\"");
    write_at_command("at+httpaction=1");

    // Wait for server
    delay(3000);
    write_at_command("at+httpread");


    debug("Successfully deployed request");
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



String form_request(String gps_in) {

    debug("In form request");
    String lat = ""; 
    String lon = "";

    
    int x, com_count, lat_index, lon_index;
    x = com_count = lat_index = lon_index = 0;
    
    debug("String length of gps_in " + gps_in.length());
    debug("Getting latitude and longitude from string: " + gps_in);
    
    for (x=0 ; x < gps_in.length(); ++x) {

        // Mark fields
        if (gps_in[x] == ',') {
            debug("Hit comma");
            com_count++;
        }

        // Third field get latitude
        if (com_count == 3) {
            debug("Debugging char: " + gps_in[x]);
            lat.concat(gps_in[x]);
        }
        // Fourth field get longitude
        if (com_count == 4) {
            debug("Debugging char: " + gps_in[x]);
            lon.concat(gps_in[x]);
        }
    }

    if (lat.length() && lon.length()) {
        debug("Got lat: " + lat);
        debug("Got lon: " + lat);
    }
    else {
        return "ERROR";
    }

    return (url + "&lat=" + lat + "&lon=" + lon);
}
