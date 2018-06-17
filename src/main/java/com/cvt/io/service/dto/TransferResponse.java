package com.cvt.io.service.dto;

import java.util.List;

public class TransferResponse extends AbstractResponse {


	private int ret;
	private String sendAddres;
	private String toAddres;
	
	public static AbstractResponse create(String sendAddres, String toAddres, int ret) {
		TransferResponse res = new TransferResponse();
		res.sendAddres = sendAddres;
		res.toAddres = toAddres;
		res.ret = ret;
		return res;
	}
	
	
}
