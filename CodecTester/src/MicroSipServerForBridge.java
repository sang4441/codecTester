import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MicroSipServerForBridge implements Runnable {

	DatagramSocket socketRecv; // 받는 소켓
	DatagramSocket socketSend; // 보내는 소켓
	DatagramPacket packetRecv; // 받는 UDP 패킷
	DatagramPacket packetSend; // 보내는 UDP 패킷
  
	byte[] pktBuffer = new byte[2048];
	int bufferSize = 2048;
	int portRecv = 5656;
	int portSend = 5656;

	boolean isRunning = false;

	public MicroSipServerForBridge() {
		try {
			socketRecv = new DatagramSocket(portRecv);
			if (portRecv == portSend) socketSend = socketRecv;
			else socketSend = new DatagramSocket(portSend);
			packetRecv = new DatagramPacket(pktBuffer, bufferSize);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		System.out.println("서버가 기동하였습니다.");
	}

	public void startServer() {
		this.isRunning = true;
		new Thread(this).start();
	}

	public void run() {
		try {
			while (isRunning) {
				socketRecv.receive(packetRecv);
				System.out.println("패킷 릴레이중, 사이즈 : " + packetRecv.getLength() + ", 받은 주소 : " + packetRecv.getAddress()+":"+packetRecv.getPort());
				packetSend = new DatagramPacket(packetRecv.getData(), packetRecv.getLength(), packetRecv.getAddress(), packetRecv.getPort());
				socketSend.send(packetSend);
				
				// 굳이 sleep 할 필요는 없다
//				try {
//					Thread.sleep(1);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// main
	public static void main(String[] args) {
		new MicroSipServerForBridge().startServer();
	}
}
