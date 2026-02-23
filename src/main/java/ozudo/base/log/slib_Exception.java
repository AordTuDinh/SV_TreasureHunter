package ozudo.base.log;

public class slib_Exception extends Exception {
    public slib_Exception(String aMsg) {
        super(aMsg);
    }

    public slib_Exception(String aMsg, Throwable aEx) {
        super(aMsg, aEx);
    }

    public slib_Exception(Throwable aEx) {
        super(aEx);
    }
}
