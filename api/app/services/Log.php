<?php

class Log {


    public static function debug($msg) {
        date_default_timezone_set("America/Los_Angeles");
        $debug_mode = true;
        if (!$debug_mode) {
            return;
        }


        $log_path = "/var/www/html/api/app/storage/logs";
        $log_handle = null;

        $log_file = $log_path . "/" . date("Y-m-d") . ".log";
        $mode = (file_exists($log_file) ? "a" : "w");

        $log_handle = fopen($log_file, $mode);
        fwrite($log_handle, "[" . date("Y-m-d H:i:s") . "] - " . $msg . "\n");
        fclose($log_handle);
    }


}