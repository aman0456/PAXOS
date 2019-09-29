public class Paxos {

    public static void main(String[] args) {
        Node<Integer, String> node = new Node<>();
        for (int i = 0; i < 10; i++){       
            String[] arguments = new String[] {"123"};
            Node.main(arguments);
        }
    }
}