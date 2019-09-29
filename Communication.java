/**
 * Communication
 */
// Make a singleton class
public class Communication {
    private static Communication comm = null;
    
    private Communication(){}
    public static Communication getInstance() 
    { if (comm == null) 
        comm = new Communication(); 
        return comm; 
    } 

    public void send(String str, int receiverId)
    {
        System.out.println("Sending ");
    }
    public String receive(){
        return "";
    }
    public void send_nack(int receiverId){
        String nack_msg = "nack";
    }
    public static void main(String[] args) {
        comm.send("",0);
    }
}
// Communication commun = new Communication();
