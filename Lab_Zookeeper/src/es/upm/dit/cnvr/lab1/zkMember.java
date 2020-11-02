package es.upm.dit.cnvr.lab1;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper; 
import org.apache.zookeeper.data.Stat;

// This is a simple application for detecting the correct processes using ZK. 
// Several instances of this code can be created. Each of them detects the 
// valid numbers.

// Two watchers are used:
// - cwatcher: wait until the session is created. 
// - watcherMember: notified when the number of members is updated

// the method process has to be created for implement Watcher. However
// this process should never be invoked, as the "this" watcher is used

public class zkMember implements Watcher{
	private static final int SESSION_TIMEOUT = 5000;

	private static String rootMembers = "/members";
	private static String aMember = "/member-";
	private String myId;
	
	// This is static. A list of zookeeper can be provided for decide where to connect
	String[] hosts = {"127.0.0.1:2181", "127.0.0.1:2181", "127.0.0.1:2181"};

	private ZooKeeper zk;
	
	public zkMember () {

		// Select a random zookeeper server
		Random rand = new Random();
		int i = rand.nextInt(hosts.length);

		// Create a session and wait until it is created.
		// When is created, the watcher is notified
		try {
			if (zk == null) {
				zk = new ZooKeeper(hosts[i], SESSION_TIMEOUT, cWatcher);
				try {
					// Wait for creating the session. Use the object lock
					wait();
					//zk.exists("/",false);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		} catch (Exception e) {
			System.out.println("Error");
		}

		// Add the process to the members in zookeeper

		if (zk != null) {
			// Create a folder for members and include this process/server
			try {
				// Create a folder, if it is not created
				String response = new String();
				Stat s = zk.exists(rootMembers, watcherMember); //this);
				if (s == null) {
					// Created the znode, if it is not created.
					response = zk.create(rootMembers, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					System.out.println(response);
				}

				// Create a znode for registering as member and get my id
				myId = zk.create(rootMembers + aMember, new byte[0], 
						Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

				myId = myId.replace(rootMembers + "/", "");

				List<String> list = zk.getChildren(rootMembers, watcherMember, s); //this, s);
				System.out.println("Created znode nember id:"+ myId );
				printListMembers(list);
			} catch (KeeperException e) {
				System.out.println("The session with Zookeeper failes. Closing");
				return;
			} catch (InterruptedException e) {
				System.out.println("InterruptedException raised");
			}

		}
	}

	// Notified when the session is created
	private Watcher cWatcher = new Watcher() {
		public void process (WatchedEvent e) {
			System.out.println("Created session");
			System.out.println(e.toString());
			notify();
		}
	};

	// Notified when the number of children in /member is updated
	private Watcher  watcherMember = new Watcher() {
		public void process(WatchedEvent event) {
			System.out.println("------------------Watcher Member------------------\n");		
			try {
				System.out.println("        Update!!");
				List<String> list = zk.getChildren(rootMembers,  watcherMember); //this);
				printListMembers(list);
			} catch (Exception e) {
				System.out.println("Exception: wacherMember");
			}
		}
	};
	
	private Watcher  watcherLocker = new Watcher() { //Watcher para el cerrojo
		public void process(WatchedEvent event) {
			System.out.println("------------------Watcher Locker------------------\n");		
			try {
				System.out.println("        Update!!");
				List<String> list = zk.getChildren(rootMembers,  watcherLocker); //this);
				printListMembers(list);
				esLider(1);
			} catch (Exception e) {
				System.out.println("Exception: wacherLocker");
			}
		}
	};
	
	@Override
	public void process(WatchedEvent event) {
		try {
			System.out.println("Unexpected invocated this method. Process of the object");
			List<String> list = zk.getChildren(rootMembers, watcherMember); //this);
			printListMembers(list);
		} catch (Exception e) {
			System.out.println("Unexpected exception. Process of the object");
		}
	}
	
	private void printListMembers (List<String> list) {
		System.out.println("Remaining # members:" + list.size());
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			System.out.print(string + ", ");				
		}
		System.out.println();
	}
	
	private String pathLider;
	private static String pathLock = "/locknode";
	private static int count = 0;
	private Integer mutex = -1;
	
	private void esLider(int valor) {
		try {
			List<String> lista = zk.getChildren(pathLock, watcherMember);
			Collections.sort(lista);
			pathLider = pathLock + "/" + lista.get(0); // path del lider en el cerrojo
			
			Stat s = zk.exists(pathLock, watcherMember);
			byte[] b = zk.getData(pathLock, watcherMember, s);
			int valorContador = (ByteBuffer.wrap(b)).getInt(); //Obtenemos el ultimo valor del contador
			
			if(lista.indexOf(myId.substring(myId.lastIndexOf('/')+1)) != 0) {
				System.out.println("El lider es: " + lista.get(0));
				System.out.println("Tengo que esperar para ser el lider...");
				synchronized(mutex) {
					zk.exists(pathLider, watcherLocker);
					mutex.wait();
				}
			} else {
				System.out.println("Soy el lider");
				count = valorContador+valor;
				s = zk.exists(pathLider, watcherMember);
				byte[] b2 = ByteBuffer.allocate(4).putInt(valorContador).array();
				zk.setData(pathLock, b2, s.getVersion());
				System.out.println("El contador vale: "+ valorContador);
				
				System.out.println("El contador ha aumentado y el lider va a dejar de ser: "+ myId);
				s = zk.exists(pathLider, watcherMember);
				System.out.println("Se va a borrar el lider");
				zk.delete(pathLider, s.getVersion());
				notify();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void addCounterValue(int valor) {
		if(zk != null) {
			try {
				// Create a folder, if it is not created
				String response = new String();
				Stat s = zk.exists(rootMembers, watcherMember); //this);
				if (s == null) {
					// Created the znode, if it is not created.
					byte[] b = ByteBuffer.allocate(4).putInt(0).array(); // Se crea un nodo lock con un contador a 0
					response = zk.create(rootMembers, b, 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					System.out.println(response);
				}

				// Create a znode for registering as member and get my id
				myId = zk.create(pathLock + "/host-", new byte[0], 
						Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

				myId = myId.replace(pathLock + "/", "");

				System.out.println("Created znode nember id:"+ myId );
				esLider(valor);
			} catch (KeeperException e) {
				System.out.println("The session with Zookeeper failes. Closing");
				return;
			} catch (InterruptedException e) {
				System.out.println("InterruptedException raised");
			}
		}
	}
	
	public static void main(String[] args) {
		zkMember zk = new zkMember();
		Thread thread = new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					System.out.println("Valor del contador: " + count);
					zk.addCounterValue(1);
					System.out.println("Valor actualizado: " + count);
					try {
						Thread.sleep(100); 			
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				System.out.println("Contador final: " + count);
				return;
			}
		};
		thread.start();
	}
}
