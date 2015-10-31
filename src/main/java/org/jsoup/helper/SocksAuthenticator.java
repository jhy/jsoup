package org.jsoup.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Authenticator}, support socks5 authentication
 *
 * @author zhou
 *
 */
public final class SocksAuthenticator extends Authenticator {
	private Map<SocketAddress, PasswordAuthentication> AUTHS = new HashMap<SocketAddress, PasswordAuthentication>();
	private Authenticator parent;
	private static Method getPasswordAuthentication;
	private static Method getRequestingURL;
	private static Method getRequestorType;

	static {
		try {
			Class<Authenticator> clazz = Authenticator.class;
			getPasswordAuthentication = clazz.getDeclaredMethod("getPasswordAuthentication");
			getPasswordAuthentication.setAccessible(true);
			getRequestingURL = clazz.getDeclaredMethod("getRequestingURL");
			getRequestingURL.setAccessible(true);
			getRequestorType = clazz.getDeclaredMethod("getRequestorType");
			getRequestorType.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public SocksAuthenticator(Authenticator parent) {
		this.parent = parent;
	}

	/**
	 * add proxy and auth
	 *
	 * @param proxy
	 * @param auth
	 */
	public void addProxyAuth(Proxy proxy, PasswordAuthentication auth) {
		AUTHS.put(proxy.address(), auth);
	}

	public static Authenticator getDefaultAuthenticator() {
		try {
			Field field = Authenticator.class.getDeclaredField("theAuthenticator");
			field.setAccessible(true);
			return (Authenticator) field.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		SocketAddress address = new InetSocketAddress(getRequestingHost(), getRequestingPort());
		return parent != null ? (PasswordAuthentication) invokeAuthenticatorMethod(getPasswordAuthentication)
				: AUTHS.get(address);
	}

	@Override
	protected URL getRequestingURL() {
		return parent != null ? (URL) invokeAuthenticatorMethod(getRequestingURL) : super.getRequestingURL();
	}

	@Override
	protected RequestorType getRequestorType() {
		return parent != null ? (RequestorType) invokeAuthenticatorMethod(getRequestorType) : super.getRequestorType();
	}

	private Object invokeAuthenticatorMethod(Method method) {
		try {
			return method.invoke(parent);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
