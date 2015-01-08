package com.linda.framework.rpc.netty.handler;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.net.AbstractRpcAcceptor;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.RpcCallListener;
import com.linda.framework.rpc.netty.RpcNettyConnector;

public class NettyRpcInBoundHandler extends SimpleChannelInboundHandler<RpcObject>{
	
	private AbstractRpcConnector parentConnector;
	
	private AbstractRpcAcceptor parentAcceptor;
	
	private ConcurrentHashMap<String, RpcNettyConnector> connectorMap = new ConcurrentHashMap<String, RpcNettyConnector>();

	private Logger logger = Logger.getLogger(NettyRpcInBoundHandler.class);
	
	private String getChannelKey(Channel channel){
		InetSocketAddress remoteAddress = (InetSocketAddress)channel.remoteAddress();
		InetAddress address = remoteAddress.getAddress();
		return address.getHostAddress()+":"+remoteAddress.getPort();
	}
	
	private RpcNettyConnector newNettyConnector(AbstractChannel channel){
		RpcNettyConnector connector = new RpcNettyConnector(channel);
		if(parentAcceptor!=null){
			parentAcceptor.addConnectorListeners(connector);
		}else if(parentConnector!=null){
			parentConnector.addConnectorListeners(connector);
		}
		return connector;
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		logger.info("channelRegistered");
		super.channelRegistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("channelActive");
		RpcNettyConnector connector = this.newNettyConnector((AbstractChannel)ctx.channel());
		connector.startService();
		String channelKey = this.getChannelKey(ctx.channel());
		System.out.println("key:"+channelKey);
		connectorMap.put(channelKey, connector);
		super.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		logger.info("channelUnregistered");
		String channelKey = this.getChannelKey(ctx.channel());
		RpcNettyConnector connector = connectorMap.get(channelKey);
		if(connector!=null){
			connector.stopService();
			connectorMap.remove(channelKey);
		}
		super.channelUnregistered(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.info("exceptionCaught");
		String channelKey = this.getChannelKey(ctx.channel());
		RpcNettyConnector connector = connectorMap.get(channelKey);
		if(connector!=null){
			connectorMap.remove(channelKey);
			connector.handleNetException((Exception)cause);
		}
		super.exceptionCaught(ctx, cause);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcObject msg)
			throws Exception {
		logger.info("channelRead0:"+msg.getIndex());
		String channelKey = this.getChannelKey(ctx.channel());
		RpcNettyConnector connector = connectorMap.get(channelKey);
		if(connector!=null){
			List<RpcCallListener> listeners = connector.getCallListeners();
			logger.info("lisnteners:"+listeners.size());
			connector.fireCall(msg);
		}else{
			logger.error("can't find connector");
		}
	}

	public AbstractRpcConnector getParentConnector() {
		return parentConnector;
	}

	public void setParentConnector(AbstractRpcConnector parentConnector) {
		this.parentConnector = parentConnector;
	}

	public AbstractRpcAcceptor getParentAcceptor() {
		return parentAcceptor;
	}

	public void setParentAcceptor(AbstractRpcAcceptor parentAcceptor) {
		this.parentAcceptor = parentAcceptor;
	}
	
}