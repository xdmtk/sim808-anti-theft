<?php
    require "services/Database.php";
    require "services/Log.php";



    function main() {

        Log::debug("Entering main function in index.php");


        // Get and log request details
        $request = process_request();
        // For SIM808 requests, assume receiving coordinates
        if ($request['from_sim'] == 'Y') {
            receive_sim_coords($_GET['data'], $request);
        }
        else {

            // Get request parameters
            $data = [
                'all' => isset($_GET['getall']),
                'get_requests' => isset($_GET['requests']),
                'get_coords' => isset($_GET['coords']),
            ];

            if ($data['get_coords']) {
                android_serve_coords($data['all']);
            }
            else if ($data['get_requests']) {
//                android_serve_requests($data);
            }
        }
    }


    /**
     * Processes incoming HTTP requests and logs collection
     * of data into requests table
     *
     * @return array - Request details
     */
    function process_request() {

        date_default_timezone_set("America/Los_Angeles");
        Log::debug("In process request");

        // Collect incoming request data
        $request = [
            'request_date' => date("Y-m-d H:i:s"),
            'request_uri' => $_SERVER['REQUEST_URI'],
            'ip' => $_SERVER['REMOTE_ADDR'],
            'user_agent' => $_SERVER['HTTP_USER_AGENT'],
            'from_sim' => isset($_GET['data']) ? 'Y' : 'N',
            'device_id' => 0,
            'valid' => (false ? 'N' : 'Y')
        ];


        Log::debug("With request data " . print_r($request, true));
        // Store request information with new request_id
        $conn = new Database();
        if ($conn->error) {
            Log::debug("Error in connecting to database: " . $conn->error);
            return;
        }
        $ret = $conn->select("requests", "request_id DESC", true);

        Log::debug("Returning from requests query with data " . print_r($ret,true));


        // Get last request_id from the first column of the first row
        $request['request_id'] = ($ret ? $ret[0]['request_id']+1 : 0);

        Log::debug("Set request ID for next request entry to " . $request['request_id']);

        // Insert new request data
        $ret = $conn->insert("requests", $request);
        if ($conn->error) {
            die("Error in connecting to database: " . $conn->error);
        }
        unset($conn);

        return $request;
    }



    /**
     * Processes incoming coordinate data by splitting CGNISNF fields
     * and inserting them into a ref copy of the master table data insert
     *
     * @return array - Request details
     */
    function process_coordinates($coordinates, & $data) {

        // Parse GPS coordinate data
        $data_fields = substr($coordinates, strpos($coordinates, ":")+1);
        $data_fields = explode(",", $data_fields);



        // Set latitude/longitude and validity based on fields seen
        // in request
        if (count($data_fields) != 5) {
            $data['latitude'] = 0;
            $data['longitude'] = 0;
            $data['valid'] = 'N';
        }
        else {
            $data['latitude'] = doubleval($data_fields[3]);
            $data['longitude'] = doubleval($data_fields[4]);
        }
    }


    /**
     *
     */
    function receive_sim_coords($coordinates, $request) {

        $conn = new Database();
        if ($conn->error) {
            die("Error connecting to database");
        }

        $ret = $conn->select("master", "id DESC", true);
        if (!$ret) {
            die("Problem retrieving row");
        }

        $data =[
            'id' => intval($ret[0]),
            'device_id' => 0,
            'request_id' => $request['request_id'],
            'access_time' => $request['request_date'],
            'latitude' => 0,
            'longitude' => 0,
            'valid' => 'Y',
        ];

        // Pass data by reference and modify latitude and longitude
        process_coordinates($coordinates, $data);

        $conn->insert("master", $data);
        if ($conn->error) {
            die("Error in connecting to database: " . $conn->error);
        }
        unset($conn);
    }




    /**
     */
    function android_serve_coords($request_all) {

        Log::debug("In android serve coord");
        $debug_mode = false;
        $conn = new Database();
        $ret = null;

        if ($debug_mode) {
            $ret = $conn->select("master", "id DESC", $request_all);
        }
        else{
            Log::debug("Selecting non debug");
            $ret = $conn->where("master", "valid = 'Y'", "id DESC", !$request_all);
        }
        if ($conn->error) {
            Log::debug("Error with fetch" . $conn->error);
        }
        echo json_encode($ret);

    }



/*
    function android_serve_requests($all) {
        global $debug_mode;

        // If making a 'getall' request, serve up the entire coordinate history
        if ($all) {

            $query = null;

            if ($debug_mode) {
                $query = "SELECT * FROM requests WHERE from_sim = 'Y';";
            }
            else {
                $query = "SELECT * FROM requests WHERE valid = 'Y' AND from_sim = 'Y';";
            }
            $res = $conn->query($query);

            if (!$res){
                debug("Query returned no results:");
                return;
            }
            $rows = [];

            // Create associative array of all rows fetched
            while ($row = $res->fetch_array(MYSQLI_ASSOC)) {
                $rows[] = $row;
            }

            echo json_encode($rows);
        }
        else {

            $query = null;

            if ($debug_mode) {
                $query = "SELECT * FROM requests WHERE from_sim = 'Y' ORDER BY request_id DESC LIMIT 1;";
            }
            else{
                $query = "SELECT * FROM requests WHERE valid = 'Y' AND from_sim ='Y' ORDER BY request_id DESC LIMIT 1 ";
            }
            // Get last row ( last coordinate put )
            $res = $conn->query($query);
            if (!$res) {
                debug("Query returned no results");
                echo json_encode($res);
                return;
            }

            echo json_encode($res->fetch_array(MYSQLI_ASSOC));
        }
        return 0;
    }
*/
    function debug($msg) {
        global $debug_mode;
        global $log_handle;

        if (true) {
            $log_date = date("Y-m-d H:i:s");
            fwrite($log_handle, "[" . $log_date . "] -> " . $msg . "\n");
        }
    }



main();
?>

