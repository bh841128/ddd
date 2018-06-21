package com.cvt.io.service.dto;

public class NewAccountResponse extends AbstractResponse {

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getRet() {
		return ret;
	}

	public void setRet(int ret) {
		this.ret = ret;
	}

	private String address;
	private int ret;

	public static AbstractResponse create(String address,int ret) {
	    NewAccountResponse res = new NewAccountResponse();
	    res.address = address;
	    res.ret=ret;
	    return res;
	}

	   
}
