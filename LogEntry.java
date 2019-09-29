// package LogEntry;
/**
 * Value
 */
public class LogEntry <I,V> {
    V value;
    I id;

    LogEntry(I id,V value){    this.id = id; this.value = value;}
    
    public I get_id(){                 return this.id; }
    public V get_value(){              return value;   }
    public void print(){
        // To print the log entry 
    }
    
}