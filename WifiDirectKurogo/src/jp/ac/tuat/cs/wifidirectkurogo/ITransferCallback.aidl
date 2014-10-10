package jp.ac.tuat.cs.wifidirectkurogo;

oneway interface ITransferCallback {
    void onProgress(int value);
    void onCancelled();
    void onCompleted();
}
