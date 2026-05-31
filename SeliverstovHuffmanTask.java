import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.PriorityQueue;

public class SeliverstovHuffmanTask {
    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("Usage: java SeliverstovHuffmanTask -c <in> <out>");
            System.err.println("   or: java SeliverstovHuffmanTask -d <in> <out>");
            System.exit(1);
        }
        try {
            if (null == args[0]) {
                System.err.println("Unknown mode: " + args[0]);
                System.exit(1);
            } else switch (args[0]) {
                case "-c":
                    Encoder.encodeFile(args[1], args[2]);
                    System.out.println("Encoded.");
                    break;
                case "-d":
                    Decoder.decodeFile(args[1], args[2]);
                    System.out.println("Decoded.");
                    break;
                default:
                    System.err.println("Unknown mode: " + args[0]);
                    System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}

class Node implements Comparable<Node>{
    private int frequency;
    private byte sym;
    Node leftSon;
    Node rightSon;
    boolean isleaf;

    Node(byte s, int f){
        this.sym = s;
        this.frequency = f;
        this.isleaf = true;
    }

    Node(int f, Node l, Node r){
        this.frequency = f;
        this.leftSon = l;
        this.rightSon = r;
        this.isleaf = false;
    }

    public int getFrequency() {
        return frequency;
    }

    public byte getSym() {
        return sym;
    }

    @Override
    public int compareTo(Node o) {
        return Integer.compare(this.frequency, o.frequency);
    }
}

class HTree{

    Node head;

    HTree(int freqs[]){
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for(int i = 0; i < 256; i++){
            if(freqs[i] > 0){
                pq.add(new Node((byte) i, freqs[i]));
            }
        }
        if(pq.isEmpty()){ 
            head = null; 
            return;
        }
        while(pq.size() > 1){
            Node left = pq.poll();
            Node right = pq.poll();
            Node parent = new Node(left.getFrequency() + right.getFrequency(), left, right);
            pq.add(parent);
        }
        head = pq.poll();
    }

    void generateCodes(String[] codes, Node currNode, String currCode){
        if(currNode.isleaf){
            codes[currNode.getSym() & 0xFF] = currCode;
        }
        if(currNode.leftSon != null){
            generateCodes(codes, currNode.leftSon, currCode + "0");
        }
        
        if(currNode.rightSon != null){
            generateCodes(codes, currNode.rightSon, currCode + "1");
        }
    }

    String[] getСodes(){
        if(head == null){
            return new String[256];
        }
        String[] codes = new String[256];
        if(head.isleaf){
            codes[head.getSym() & 0xFF] = "";
        }
        else{
            generateCodes(codes, head, "");
        }
        return codes;
    }
} 

class BitOutputStream {

    private int buffer;
    private int bitsInBuffer;
    private ByteArrayOutputStream outputStream;
    private int validBits;

    public BitOutputStream() {
        buffer = 0;
        bitsInBuffer = 0;
        validBits = 0;
        outputStream = new ByteArrayOutputStream();
    }
    
    void writeBit(int bit){
        if(bit < 0 || bit > 1){
            return;
        }
        buffer = (buffer << 1) | (bit & 1);
        bitsInBuffer++;
        if(bitsInBuffer == 8){
            outputStream.write(buffer);
            buffer = 0;
            bitsInBuffer = 0;
        }
    }

    void flush(){
        if(bitsInBuffer > 0){
            buffer <<= (8 - bitsInBuffer);
            validBits = bitsInBuffer;
            outputStream.write(buffer);
            buffer = 0;
            bitsInBuffer = 0;
        }
    }

    public int getValidBits() {
        return validBits;
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}

class Encoder{
    static void encodeFile(String inputPath, String outputPath) throws IOException {
        byte[] inputData;
        try (FileInputStream fis = new FileInputStream(inputPath)) {
            inputData = fis.readAllBytes();
        }
        int[] frequencies = new int[256];
        for (byte b : inputData) {
            frequencies[b & 0xFF]++;
        }
        HTree tree = new HTree(frequencies);
        BitOutputStream bitOut = new BitOutputStream();
        String[] codes = tree.getСodes();

        for (byte b : inputData) {
             String code = codes[b & 0xFF];
             for (char ch : code.toCharArray()){
                bitOut.writeBit(ch == '1' ? 1 : 0);
             }
        }
        bitOut.flush();

        byte[] encodedData = bitOut.getBytes();
        int validBits = bitOut.getValidBits();

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputPath))) {
            // Количество уникальных символов
            int uniqueCount = 0;
            for (int f : frequencies) if (f > 0) uniqueCount++;
            dos.writeInt(uniqueCount);

            // Запись пар (символ, частота)
            for (int i = 0; i < 256; i++) {
                if (frequencies[i] > 0) {
                    dos.writeByte(i);
                    dos.writeInt(frequencies[i]);
                }
            }

            dos.writeByte(validBits);
            dos.write(encodedData);
        }
    }
}

class BitInputStream {
    private byte[] data;
    private int byteIndex;
    private int bitPos;
    private int totalBits;
    private int bitsRead;

    BitInputStream(byte[] data, int validBits) {
        this.data = data;
        this.bitsRead = 0;
        this.byteIndex = 0;
        this.bitPos = 0;
        this.totalBits = (data.length == 0) ? 0 : (data.length - 1) * 8 + validBits;
    }

    int readBit() {
        if (bitsRead >= totalBits){
            return -1;
        }
        if (byteIndex >= data.length){
            return -1;  
        } 
        int currentByte = data[byteIndex] & 0xFF;
        int bit = (currentByte >> (7 - bitPos)) & 1;
        bitPos++;
        bitsRead++;
        if (bitPos == 8) {
            bitPos = 0;
            byteIndex++;
        }
        return bit;
    }
}

class Decoder {
    static void decodeFile(String inputPath, String outputPath) throws IOException {

        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputPath))) {
            int uniqueCount = dis.readInt();
            if (uniqueCount == 0) {
                try (FileOutputStream fos = new FileOutputStream(outputPath)) { 
                    //создание пустого файла
                }
                return;
            }

            int[] freqs = new int[256];
            for (int i = 0; i < uniqueCount; i++) {
                int symbol = dis.readUnsignedByte();
                int freq = dis.readInt();
                freqs[symbol] = freq;
            }

            int validBits = dis.readUnsignedByte();
            byte[] encodedData = dis.readAllBytes(); // остаток файла

            HTree tree = new HTree(freqs);
            Node root = tree.head;

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                if (root.isleaf) {
                    byte sym = root.getSym();
                    int totalSymbols = 0;
                    for (int f : freqs) totalSymbols += f;
                    for (int i = 0; i < totalSymbols; i++) fos.write(sym);
                    return;
                }

                BitInputStream bitIn = new BitInputStream(encodedData, validBits);
                Node current = root;
                int bit;
                while ((bit = bitIn.readBit()) != -1) {
                    if (bit == 0) current = current.leftSon;
                    else current = current.rightSon;

                    if (current.isleaf) {
                        fos.write(current.getSym());
                        current = root;
                    }
                }
            }
        }
    }
}