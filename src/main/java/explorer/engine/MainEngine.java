package explorer.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import explorer.protocol.Protocol;


public class MainEngine {
	private final int THREADS;
	private ExecutorService exec;
	private Sha256Hash[] firstBlock;
	private Sha256Hash[] lastBlock;
	HikariConfig config;

	public MainEngine(String inputFile, Protocol platform, int firstChunk, int lastChunk){
		THREADS = lastChunk-firstChunk+1;							// Setting number of workers
		this.exec = Executors.newFixedThreadPool(THREADS);			// Initializing thread executor service
		this.firstBlock = new Sha256Hash[THREADS];					// For each thread indicates the first (younger) block to explore
		this.lastBlock = new Sha256Hash[THREADS];					// For each thread indicates the last (older) block to explore

		Properties prop = new Properties();
		try(InputStream input = new FileInputStream("resources/config.properties")){

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			String dbName = prop.getProperty("dbname");
			String dbUser = prop.getProperty("dbuser");
			String dbPassword = prop.getProperty("dbpassword");

			this.config = new HikariConfig();
		    config.setMaximumPoolSize(100);
		    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		    config.setJdbcUrl("jdbc:mysql://localhost:3306/?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC");
			config.addDataSourceProperty("databaseName", dbName);
			config.addDataSourceProperty("user", dbUser);
			config.addDataSourceProperty("password", dbPassword);

			// Open input file (containing chunks) and set first block and last block for each thread.
			File f = new File(inputFile);
			try(Scanner s = new Scanner(f)){
				int indexFile = 0;
				int indexThread = 0;

				// Skip chunks not requested
				while(indexFile < firstChunk){
					s.nextLine();
					indexFile++;
				}

				// Load data for requested chunks
				while(indexFile <= lastChunk){
					this.firstBlock[indexThread] = new Sha256Hash(s.next());
					this.lastBlock[indexThread] = new Sha256Hash(s.next());
					s.nextLine();
					indexFile++;
					indexThread ++;
				}

				try(HikariDataSource ds = new HikariDataSource(config)){
					// Creating workers
					for(int i=0; i<THREADS; i++){
						exec.execute(new Engine(MainNetParams.get(), ds, i, platform, firstBlock[i], lastBlock[i]));		
					}

					// Waiting threads
					exec.shutdown();
					while(! exec.awaitTermination(1, TimeUnit.MINUTES))
						System.out.println("Waiting another minute");
				
				}catch(Exception e){
					e.printStackTrace();
				}

			} catch(FileNotFoundException e){
				e.printStackTrace();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception{
		
		System.out.println("Starting Analysis: " + new Timestamp(new java.util.Date().getTime()));
		
		//TODO add input validation
		MainEngine e = new MainEngine(args[0], Protocol.getPlatform(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		
		System.out.println("Analysis concluded: " + new Timestamp(new java.util.Date().getTime()));
	}
}