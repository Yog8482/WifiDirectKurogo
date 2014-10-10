package jp.ac.tuat.cs.wifidirectkurogo.connection;

/**
 * ITransferCallback そのままでは使いづらいので，このインタフェースでラップする
 */
public interface TransferListener {
	void onProgress(int value);
    void onCancelled();
    void onCompleted();
}
