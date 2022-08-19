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

	public Integer getNetThreads() {
		return netThreads;
	}

	public void setNetThreads(Integer netThreads) {

		this.netThreads = netThreads;
	}

	public HashMap<String, Class<ProbeHandlerTerm>> getProbes() {
		return probes;
	}

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

	public String getProbeInetHost() {
		return probeInetHost;
	}

	public void setProbeInetHost(String probeInetHost) {
		this.probeInetHost = probeInetHost;
	}

	public int getProbeInetPort() {
		return probeInetPort;
	}

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
