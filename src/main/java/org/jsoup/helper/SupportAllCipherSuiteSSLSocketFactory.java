package org.jsoup.helper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SupportAllCipherSuiteSSLSocketFactory extends SSLSocketFactory {
	
	private final SSLSocketFactory delegate;

	public SupportAllCipherSuiteSSLSocketFactory(SSLSocketFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return setupSupportedCipherSuites(this.delegate);
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return setupSupportedCipherSuites(this.delegate);
	}

	@Override
	public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {

		Socket socket = this.delegate.createSocket(arg0, arg1);
		String[] cipherSuites = setupSupportedCipherSuites(delegate);
		((SSLSocket) socket).setEnabledCipherSuites(cipherSuites);

		return socket;
	}

	@Override
	public Socket createSocket(InetAddress arg0, int arg1) throws IOException {

		Socket socket = this.delegate.createSocket(arg0, arg1);
		String[] cipherSuites = setupSupportedCipherSuites(delegate);
		((SSLSocket) socket).setEnabledCipherSuites(cipherSuites);

		return socket;
	}

	@Override
	public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3) throws IOException {

		Socket socket = this.delegate.createSocket(arg0, arg1, arg2, arg3);
		String[] cipherSuites = setupSupportedCipherSuites(delegate);
		((SSLSocket) socket).setEnabledCipherSuites(cipherSuites);

		return socket;
	}

	@Override
	public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3)
			throws IOException, UnknownHostException {

		Socket socket = this.delegate.createSocket(arg0, arg1, arg2, arg3);
		String[] cipherSuites = setupSupportedCipherSuites(delegate);
		((SSLSocket) socket).setEnabledCipherSuites(cipherSuites);

		return socket;
	}

	@Override
	public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {

		Socket socket = this.delegate.createSocket(arg0, arg1, arg2, arg3);
		String[] cipherSuites = setupSupportedCipherSuites(delegate);
		((SSLSocket) socket).setEnabledCipherSuites(cipherSuites);

		return socket;
	}
	
	private static String[] setupSupportedCipherSuites(SSLSocketFactory sslSocketFactory){
		String[] defaultCipherSuites = sslSocketFactory.getDefaultCipherSuites();

		ArrayList<String> suitesList = new ArrayList<String>(Arrays.asList(defaultCipherSuites));
		suitesList.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
		suitesList.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
		//suitesList.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
		//suitesList.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
		//suitesList.add("TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256");
		//suitesList.add("TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256");
		
		suitesList.add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA");
		suitesList.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
		//suitesList.add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA");
		//suitesList.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
		//suitesList.add("TLS_RSA_WITH_AES_128_GCM_SHA256");
		//suitesList.add("TLS_RSA_WITH_AES_256_GCM_SHA384");
		//suitesList.add("TLS_RSA_WITH_AES_128_CBC_SHA");
		//suitesList.add("TLS_RSA_WITH_AES_256_CBC_SHA");
		//suitesList.add("TLS_RSA_WITH_3DES_EDE_CBC_SHA");

		return suitesList.toArray(new String[suitesList.size()]);
	}
}
