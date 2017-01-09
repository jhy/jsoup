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
		return suitesList.toArray(new String[suitesList.size()]);
	}
}
