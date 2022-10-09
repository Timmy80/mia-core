package com.github.timmy80.mia.core;

import java.util.HashMap;


/**
 * Parameters of an {@link ApplicationContext}
 * 
 * @author anthony
 *
 */
public class ApplicationContextParams implements Cloneable {

	/**
	 * Number of Threads allocated to the networking stack.
	 */
	private Integer netThreads = null;

	private HashMap<String, Class<ProbeHandlerTerm>> probes = new HashMap<>();

	private String probeInetHost = "0.0.0.0"; // all by default

	private int probeInetPort = 0; // disabled by default
	
	/**
	 * Default constructor
	 */
	public ApplicationContextParams() {
		
	}

	/**
	 * Number of threads allocated to Netty's eventLoopGroup
	 * @return at least 1
	 */
	public Integer getNetThreads() {
		return netThreads;
	}

	/**
	 * Set Number of threads allocated to Netty's eventLoopGroup
	 *
	 * @param netThreads at least 1
	 */
	public void setNetThreads(Integer netThreads) {
		if(netThreads < 1)
			throw new IllegalArgumentException("netThreads canoot be less than 1");
		this.netThreads = netThreads;
	}

	/**
	 * Specification of the probe handlers
	 * @return A Map where key=path, path=handler class.
	 */
	public HashMap<String, Class<ProbeHandlerTerm>> getProbes() {
		return probes;
	}

	/**
	 * Add a probe to this {@link ApplicationContext}
	 * @param <T> Type of an implementation of {@link ProbeHandlerTerm}
	 * @param path Path of the probe
	 * @param probeHandler A class implementing {@link ProbeHandlerTerm}
	 */
	@SuppressWarnings("unchecked")
	public <T extends ProbeHandlerTerm> void addProbe(String path, Class<T> probeHandler) {
		if (!path.startsWith("/"))
			throw new IllegalArgumentException("A probe path must start with a '/'.");

		this.probes.put(path, (Class<ProbeHandlerTerm>) probeHandler);
	}
	
	/**
	 * Add the {@link DefaultLivenessProbe} on /healthz
	 */
	public void addDefaultLivenessProbes() {
		addProbe("/healthz", DefaultLivenessProbe.class);
	}
	
	/**
	 * Add the {@link DefaultReadinessProbe} on /readiness
	 */
	public void addDefaultReadynessProbes() {
		addProbe("/readiness", DefaultReadinessProbe.class);
	}

	/**
	 * Get the host IP listened to for probes.
	 * @return An IP represented as a String
	 */
	public String getProbeInetHost() {
		return probeInetHost;
	}
	
	/**
	 * Set the host IP listened to for probes.
	 * @param probeInetHost An IP represented as a String
	 */
	public void setProbeInetHost(String probeInetHost) {
		this.probeInetHost = probeInetHost;
	}

	/**
	 * Get the port listened to for probes
	 * @return A TCP port number
	 */
	public int getProbeInetPort() {
		return probeInetPort;
	}

	/**
	 * Set the port listened to for probes
	 * @param probeInetPort A TCP port number
	 */
	public void setProbeInetPort(int probeInetPort) {
		this.probeInetPort = probeInetPort;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() {
		ApplicationContextParams o = null;
		try {
			o = (ApplicationContextParams) super.clone();
		} catch (CloneNotSupportedException e) {
			// Impossible because we are implementing Cloneable
		}

		// clone the probes
		o.probes = (HashMap<String, Class<ProbeHandlerTerm>>) probes.clone();

		return o; // return the clone
	}

}
