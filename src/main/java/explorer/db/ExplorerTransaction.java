package explorer.db;

import java.nio.ByteBuffer;
import java.util.List;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

import explorer.protocol.Codes;
import explorer.protocol.Protocol;
import explorer.utils.ConvertUtils;
import explorer.utils.WalletUtils;

public class ExplorerTransaction {
	
	private String protocol;
	private String data;
	private Boolean isOpReturn;
	private String txHash;
	private String blockHash;
	private Coin fee;
	private Address sender;
	private Address reference;
	
	private double tetherAmount;
	private String transactionType;
	
	public ExplorerTransaction(Block block, Transaction tx){
		isOpReturn = false;
		String value = evaluateType(block, tx);
		if(isOpReturn)
			data = extractData(value);
	}
	
	
	
	public Boolean isOpReturn(){
//		if(this.protocol == null) 
//			return false;
//		return true;
		return isOpReturn;
	}
	
	
	
	public String getName(){
		return this.protocol;
	}
	
	
	
	public String getData(){
		return this.data;
	}
	
	public String evaluateType(Block block, Transaction tx) {
		
		List<TransactionOutput> listOutputs = tx.getOutputs();
		String tempString=null;
		
		// Check if the transactions uses OP_RETURN
		for(TransactionOutput output : listOutputs){
			try{
				tempString = (new Script(output.getScriptBytes())).toString();
				if(tempString.startsWith(Codes.RETURN)){
					this.isOpReturn = true;
					break;
				}
			} catch(ScriptException e){}
		}

		if(this.isOpReturn == false) return null;


		try{
			if(!tempString.contains(Codes.BRACKET)){
				this.protocol = Protocol.EMPTY;
				this.isOpReturn = false;				// FAKE OP_RETURN = FALSE BECAUSE WE ONLY WANT OMNI TETHER DATA
				return tempString;
			}
			
//			if(tempString.contains(Codes.BRACKET + Codes.ASCRIBE_CODE1)){
//				this.protocol = Protocol.ASCRIBE;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.BITPROOF_CODE1)){
//				this.protocol = Protocol.BITPROOF;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.BLOCKAI_CODE1)){
//				this.protocol = Protocol.BLOCKAI;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.BLOCKSIGN_CODE1)){
//				this.protocol = Protocol.BLOCKSIGN;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.BLOCKSTORE_CODE1) || 
//			   tempString.contains(Codes.BRACKET + Codes.BLOCKSTORE_CODE2) || 
//			   tempString.contains(Codes.BRACKET + Codes.BLOCKSTORE_CODE3)){
//				this.protocol = Protocol.BLOCKSTORE;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.COINSPARK_CODE1)){
//				this.protocol = Protocol.COINSPARK;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.COLU_CODE1)){
//				this.protocol = Protocol.COLU;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.CRYPTOCOPYRIGHT_CODE1) || 
//			   tempString.contains(Codes.BRACKET + Codes.CRYPTOCOPYRIGHT_CODE2)){
//				this.protocol = Protocol.CRYPTOCOPYRIGHT;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.ETERNITYWALL_CODE1)){
//				this.protocol = Protocol.ETERNITYWALL;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.FACTOM_CODE1) || 
//			   tempString.contains(Codes.BRACKET + Codes.FACTOM_CODE2) || 
//			   tempString.contains(Codes.BRACKET + Codes.FACTOM_CODE3) || 
//			   tempString.contains(Codes.BRACKET + Codes.FACTOM_CODE4)){
//				this.protocol = Protocol.FACTOM;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.LAPREUVE_CODE1)){
//				this.protocol = Protocol.LAPREUVE;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.MONEGRAPH_CODE1)){
//				this.protocol = Protocol.MONEGRAPH;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.NICOSIA_CODE1)){
//				this.protocol = Protocol.NICOSIA;
//				return tempString;
//			}
			
			if(tempString.contains(Codes.BRACKET + Codes.OMNI_CODE1)){
				
				if(tempString.matches(Codes.OMNI_CODE_TETHER_REGEX)) {
					
					if(tx.getHashAsString().equalsIgnoreCase("f8eb444bdc11b4d85dfe44fb6aa2eb20628a93d23fde84ec60835a25acb617fd")) {
						System.out.println("Here...");
					}
					
					this.protocol = Protocol.OMNI_TETHER;			
	//				System.out.println("Adding transaction " +  tx.getHashAsString() + " - " + WalletUtils.getWalletAddressOfSender(tx) + " - " + WalletUtils.getWalletAddressOfReceiver(tx, false));
					this.sender = WalletUtils.getWalletAddressOfSender(tx);
					this.reference = WalletUtils.getWalletAddressOfReceiver(tx, this.sender);
					this.setTxHash(tx.getHashAsString());
					
					String str = tempString.split(Codes.OMNI_CODE_TETHER_REGEX_SPLITTER)[1].replace("]", "");
					ByteBuffer wrapped = ByteBuffer.wrap(ConvertUtils.hexToBytes(str)); // big-endian by default
					double num = wrapped.getLong()/100000000d;
					this.tetherAmount = num;
					
					// Caso estranho
					// Essa TX abaixo é uma "Crowdsale Purchase" mas o código do TransactionType é 0
					// então nosso código salvaria no DB como "Simple Send"
					// Ver como diferenciar esse caso
					// https://www.omniexplorer.info/tx/8669aa4bd4f69ebd1e84d9a38f0f3412e97a995aabc312de8a25650449151d9d
					int txType = Integer.parseInt(tempString.split(Codes.OMNI_CODE_TETHER)[1].substring(0, 2) , 16);
					switch(txType) {
					
						case 0:	this.transactionType = "Simple Send"; break;
						case 3: this.transactionType = "Send To Owners"; break;
						case 20: this.transactionType = "Sell Coins for Bitcoins (currency trade offer)"; break;
						case 21: this.transactionType = "Offer/Accept Omni Protocol Coins for Another Omni Protocol Currency (currency trade offer)"; break;
						case 22: this.transactionType = "Purchase Coins with Bitcoins (accept currency trade offer)"; break;
						case 51: this.transactionType = "Create a Property via Crowdsale with Variable number of Tokens"; break;
						case 52: this.transactionType = "Promote a Property"; break;
						case 53: this.transactionType = "Close a Crowdsale Manually"; break;
						case 54: this.transactionType = "Create a Managed Property with Grants and Revocations"; break;
						case 55: this.transactionType = "Grant Property Tokens"; break;
						case 56: this.transactionType = "Revoke Property Tokens"; break;
						case 70: this.transactionType = "Change Property Issuer on Record"; break;
						default: this.transactionType = "UNKNOWN";
					
					}
							
					return tempString;
				}
			
			}
			
//			if(tempString.contains(Codes.BRACKET + Codes.OMNI_CODE1)){
//				this.protocol = Protocol.OMNI;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.OPENASSETS_CODE1)){
//				this.protocol = Protocol.OPENASSETS;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.ORIGINALMY_CODE1)){
//				this.protocol = Protocol.ORIGINALMY;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.PROOFOFEXISTENCE_CODE1)){
//				this.protocol = Protocol.PROOFOFEXISTENCE;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.PROVEBIT_CODE1)){
//				this.protocol = Protocol.PROVEBIT;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.REMEMBR_CODE1) || 
//			   tempString.contains(Codes.BRACKET + Codes.REMEMBR_CODE2)){
//				this.protocol = Protocol.REMEMBR;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.SMARTBIT_CODE1)){
//				this.protocol = Protocol.SMARTBIT;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.STAMPD_CODE1)){
//				this.protocol = Protocol.STAMPD;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.STAMPERY_CODE1) || 
//			   tempString.contains(Codes.BRACKET + Codes.STAMPERY_CODE2) ||
//			   tempString.contains(Codes.BRACKET + Codes.STAMPERY_CODE3) ||
//			   tempString.contains(Codes.BRACKET + Codes.STAMPERY_CODE4) ||
//			   tempString.contains(Codes.BRACKET + Codes.STAMPERY_CODE5)){
//				this.protocol = Protocol.STAMPERY;
//				return tempString;
//			}
//			
//			if(tempString.contains(Codes.BRACKET + Codes.TRADLE_CODE1)){
//				this.protocol = Protocol.TRADLE;
//				return tempString;	
//			}	
//
//
//			String key = tx.getInputs().get(0).getOutpoint().toString().substring(0, 64);			
//			String message = extractData(tempString);
//			RC4 rc4 = new RC4(ConvertUtils.hexToBytes(key));
//			String result = (ConvertUtils.bytesToHex(rc4.decrypt(ConvertUtils.hexToBytes(message)))).toLowerCase();
//			
//			if(result.startsWith(Codes.COUNTERPARTY_CODE1)){
//				this.protocol = Protocol.COUNTERPARTY;
//				return tempString;	
//			}
			
		} catch(ScriptException e){}
		
		this.isOpReturn = false;				// FAKE OP_RETURN = FALSE BECAUSE WE ONLY WANT OMNI TETHER DATA
		this.protocol = Protocol.UNKNOWN;
		return tempString;
	}
	

	public String extractData(String value){
		int v1 = value.indexOf("[");
		int v2 = value.indexOf("]");
		if((v1 == -1) || (v2 == -1)) 
			return "";
		else
			return value.substring(v1+1, v2);
	}
	
	public String getProtocol(){
		return protocol;
	}
	
	public String getTxHash() {
		return txHash;
	}

	public void setTxHash(String txHash) {
		this.txHash = txHash;
	}

	public String getBlockHash() {
		return blockHash;
	}

	public void setBlockHash(String blockHash) {
		this.blockHash = blockHash;
	}
		
	public Coin getFee() {
		return fee;
	}

	public void setFee(Coin fee) {
		this.fee = fee;
	}

	public Address getSender() {
		return sender;
	}

	public void setSender(Address sender) {
		this.sender = sender;
	}

	public Address getReference() {
		return reference;
	}

	public void setReference(Address reference) {
		this.reference = reference;
	}

	public double getTetherAmount() {
		return tetherAmount;
	}

	public void setTetherAmount(double tetherAmount) {
		this.tetherAmount = tetherAmount;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((txHash == null) ? 0 : txHash.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExplorerTransaction other = (ExplorerTransaction) obj;
		if (txHash == null) {
			if (other.txHash != null)
				return false;
		} else if (!txHash.equals(other.txHash))
			return false;
		return true;
	}
}
