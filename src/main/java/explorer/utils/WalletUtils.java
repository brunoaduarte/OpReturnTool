package explorer.utils;

import java.util.List;

import javax.annotation.Nullable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptChunk;

public class WalletUtils {
	
	// THIS IF A FUCKED UP WORKAROUND, FIX ASAP
	@Nullable
	public static Address getWalletAddressOfReceiver(final Transaction tx, final Address senderAddress) {
		
//		long vOut = tx.getInput(0).getOutpoint().getIndex();
//		List<TransactionInput> inputs = tx.getInputs();
//		int idx = 0;
		
//		try {
//			final Script script = outputs.get((int) vOut).getScriptPubKey();
//			Address receiverAddress = script.getToAddress(MainNetParams.get(), true);
//		} catch (final ScriptException x) {
//			// swallow
//		}

		
		for (final TransactionOutput output : tx.getOutputs()) {
			
			try {
				
				final Script script = output.getScriptPubKey();				
				boolean sentToAddress = script.isSentToAddress();		
				boolean payToScriptHash = script.isPayToScriptHash();
				
//				if (!payToScriptHash) {
				
					Address receiverAddress = script.getToAddress(MainNetParams.get(), true);
					if(receiverAddress.equals(senderAddress))
						continue;
					
//					if(senderAddress.isP2SHAddress()) {
//						System.out.print(" - " + tx.getOutputs().size() + " - " + vOut + " - ");
//					}
					
					return receiverAddress;
					
//				}
				
			} catch (final ScriptException x) {

			}
		}

		return null;
	}

	@Nullable
	public static Address getWalletAddressOfSender(final Transaction tx) {

		Address fromAddress = null;
		
		for (final TransactionInput ti : tx.getInputs()) {
			
			try {
				Script scriptSig = ti.getScriptSig();
				List<ScriptChunk> chunks = scriptSig.getChunks();
				
	            byte[] pubKey = chunks.get(chunks.size() - 1).data;
	            byte[] pubKeyHash = Utils.sha256hash160(pubKey);

	            if (chunks.size() > 2 || chunks.size() == 1) { // assume P2SH
	                fromAddress = Address.fromP2SHHash(MainNetParams.get(), pubKeyHash);
	                if(chunks.size() < 2) {
	                	System.out.print("WATCH -> ");
	                }
	                	
	            } else if (chunks.size() == 2) { // assume P2PKH
	                fromAddress = new Address(MainNetParams.get(), pubKeyHash);
	            }
				
				return fromAddress;
			} catch (final ScriptException x) {
				System.out.println(x.getMessage());
			}
		}

		return null;
	}

}
