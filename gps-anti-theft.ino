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

char * lon_url = "&lon=";
char * lat_url = "&lat=";
int L_RED = 2;
int L_YEL = 3;
int L_GRE = 4;


SoftwareSerial sim_808(RX_SS, TX_SS);

void setup()
{
    pinMode(L_RED, OUTPUT);
    pinMode(L_YEL, OUTPUT);
    pinMode(L_GRE, OUTPUT);

    // Wait 10 seconds on start up, then turn green LED on
    delay(10000);
    digitalWrite(L_GRE, HIGH);

    // Start computer serial and flash yellow
    delay(1000);
    Serial.begin(9600);
    flash_pin(L_YEL);
   
    // Start SIM808 serial and flash red
    sim_808.begin(9600);
    flash_pin(L_RED);
   
    // Turn on GPS and flash green, then permanent green
    delay(1000);
    sim_808.write("at+cgnspwr=1\n");
    flash_pin(L_GRE);
    digitalWrite(L_GRE, HIGH);
    

    // Initiate GSM 
    delay(500);
    sim_808.write("at+sapbr=3,1,\"CONTYPE\",\"GPRS\"\n");
    flash_pin(L_GRE);
    // Toss out first read ( possible junk info on startup)
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }

    delay(500);
    sim_808.write("at+sapbr=3,1,\"APN\",\"wholesale\"\n");
    flash_pin(L_GRE);
    // Toss out first read ( possible junk info on startup)
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    
    delay(500);
    sim_808.write("at+sapbr=1,1\n");
    flash_pin(L_GRE);
    // Toss out first read ( possible junk info on startup)
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }

    delay(500);
    sim_808.write("at+sapbr=2,1\n");
    flash_pin(L_GRE);
    // Toss out first read ( possible junk info on startup)
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    
    delay(500);
    sim_808.write("at+httpinit\n");
    flash_pin(L_GRE);
    
    // Toss out first read ( possible junk info on startup)
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    
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



void loop()
{
    Serial.println("In loop");
    for (int f = 0; f < 15; f++) {
        flash_pin(L_RED);
        delay(100);
        flash_pin(L_YEL);
        delay(100);
    }
    digitalWrite(L_GRE, HIGH);

    // Initialize gps read array
    Serial.println("About to initialize gps in");
    char gps_in[512];
    for (int x = 0; x < 512; ++x) {
        gps_in[x] = '\0';
    }
    
    // Issue GPS get 
    delay(1000);
    sim_808.write("at+cgnsinf\n");
    flash_pin(L_RED);
    
    int x = 0;
    while (sim_808.available()) {
        gps_in[x] = sim_808.read();
        if (gps_in[x] == -1) {
            break;
        }
        Serial.write(gps_in[x]);
        flash_pin(L_YEL);
        x++;
    }
    Serial.println("About to enter form request");
    
    char * https_request = form_request(gps_in);

    if (!strcmp(https_request, "ERROR")) {
        Serial.println("Couldn't get GPS coordinates, probably not warmed up yet");
        for (int r = 0; r < 20; r++) {
            flash_pin(L_RED);
        }
        return;
    }
    else {
        Serial.println(https_request);
    }
    sim_808.write("at+httppara=\"CID\",1\n");
    flash_pin(L_RED);
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    sim_808.write("at+httpssl=1\n");
    flash_pin(L_RED);
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    sim_808.write("httppara=\"URL\",\"");
    flash_pin(L_RED);
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    sim_808.write(https_request);
    flash_pin(L_RED);
    sim_808.write("\"\n");
    flash_pin(L_RED);
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    sim_808.write("at+httpaction=1\n");
    flash_pin(L_RED);
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    sim_808.write("at+httpread\n");
    flash_pin(L_RED);


    // Toss out HTTP response, flash yellow 
    while (sim_808.available()) {
        char c = sim_808.read();
        Serial.write(c);
        flash_pin(L_YEL);
    }
    delay(5000);
    
}

char * form_request(char * gps_in) {
    char lat[32];
    char lon[32];
    Serial.println("In form request");
    for (int y = 0; y < 32; ++y) {
        lat[y] = '\0';
        lon[y] = '\0';
    }
    
    Serial.println("Zeroed out lat and lon");
    int x, com_count, lat_index, lon_index;
    x = com_count = lat_index = lon_index = 0;


    Serial.println("about to split CSV");
    // Split CSV
    for (; x < 512; ++x) {
        // Mark fields
        if (gps_in[x] == ',') {
            com_count++;
        }

        // Third field get latitude
        if (com_count == 3) {
            lat[lat_index++] = gps_in[x];
        }
        // Fourth field get longitude
        if (com_count == 4) {
            lon[lon_index++] = gps_in[x];
        }
    }
    if ((lat_index < 2) || (lon_index < 2)) {
        return "ERROR";
    }
    Serial.print("Lat: ");
    Serial.println(lat);
    Serial.print("Lon: ");
    Serial.println(lon);
    Serial.println("About to do concatenations");

    char * n_lon_url = strcat(lon_url, lon);
    Serial.println("Concat 1");
    char * n_lat_url = strcat(lat_url, lon);
    Serial.println("Concat 2");
    char * n_url = strcat(url, n_lon_url);
    Serial.println("Concat 3");
    char * n_n_url = strcat(n_url, lat_url);
    Serial.println("Concat 4");
    Serial.println(n_n_url);    
    return n_n_url;
}





