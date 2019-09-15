<?php

class Database
{
    public $error;
    private $connection;

    public function __construct() {

        $ENV_PATH =  "/var/www/html/api/.env";
        $env_arr = file($ENV_PATH);

        Log::debug("In new db construct");

        $m_servername = trim(substr($env_arr[0], strpos($env_arr[0], '=')+1));
        $m_username = trim(substr($env_arr[1], strpos($env_arr[1], '=')+1));
        $m_password = trim(substr($env_arr[2], strpos($env_arr[2], '=')+1));
        $m_dbname = trim(substr($env_arr[3], strpos($env_arr[3], '=')+1));


        // Connect and check for errors
        $this->connection = new mysqli($m_servername, $m_username, $m_password, $m_dbname);
        if ($this->connection->connect_error) {
            Log::debug("error on connect" . $this->connection->connect_error);
            $this->error = $this->connection->connect_error;
        }
        Log::debug("Returned succes");

    }

    public function __destruct() {
        if ($this->connection) {
            $this->connection->close();
        }
    }

    public function insert($table, $keys_values) {

        $query = "INSERT INTO " . $table;

        $keys = "(";
        $values = "(";
        $kv_count = count($keys_values);
        $x = 0;

        foreach ($keys_values as $key => $value) {

            $keys = $keys . $key;
            $values = $values . "\"" . $value . "\"";

            if ($x != $kv_count-1) {
                $keys = $keys . ", ";
                $values = $values . ", ";
            }
            $x++;
        }

        $keys = $keys . ")";
        $values = $values . ")";

        $query = $query . " " . $keys . " VALUES " . $values . ";";

        Log::debug("About to execute query string " . $query);
        $this->connection->query($query);
        if ($this->connection->error) {
            Log::debug("Query error: " . $this->connection->error);
            return false;
        }
        return true;

    }


    public function where($table, $conditions, $order_by = false, $first = false) {

        if (!count($conditions)) {
            return false;
        }
        $query = null;
        if (!is_array($conditions)) {
            $query = "SELECT * FROM " . $table . " WHERE " . $conditions;
        }
        else {
            $query = "SELECT * FROM " . $table . " WHERE " . $conditions[0];
        }

        if (count($conditions) > 1) {

            foreach ($conditions as $condition) {
                $query = $query . " AND " . $condition;
            }
        }
        if ($order_by) {
            $query = $query . " ORDER BY " .  $order_by;
        }
        if ($first) {
            $query = $query . " LIMIT 1";
        }
        $query = $query . ";";

        Log::debug("about to execute where class " . $query);

        $res = $this->connection->query($query);
        if ($this->connection->error) {
            Log::debug("Error in fetch " . $this->connection->error);
        }
        if (!$res) {
            return false;
        }


        $rows = [];
        while ($row = $res->fetch_array(MYSQLI_ASSOC)) {
            $rows[] = $row;
        }
        return $rows;
    }


    public function select($table, $order_by = false, $first = false) {

        $query = "SELECT * FROM " . $table;
        $query = $query . ($order_by ? " ORDER BY " . $order_by : "");
        $query = $query . ($first ? " LIMIT 1" : "");
        $query = $query . ";";

        Log::debug("in select about to query with " . $query);
        $res = $this->connection->query($query);


        if ($this->connection->error) {
            Log::debug("Error in fetch " . $this->connection->error);
        }
        if (!$res) {
            Log::debug("returned nothign");
            return false;
        }

        $rows = [];
        while ($row = $res->fetch_array(MYSQLI_ASSOC)) {
            $rows[] = $row;
        }
        Log::debug("Returned rows" . print_r($rows, true));
        return $rows;

    }

    public function raw_sql($sql) {
       $this->connection->query($sql);
        if ($this->connection->error) {
            Log::debug("Error in raw sql" . $this->connection->error);
        }
    }





}