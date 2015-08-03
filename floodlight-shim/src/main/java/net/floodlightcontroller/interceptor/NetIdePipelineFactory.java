package net.floodlightcontroller.interceptor;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.util.CharsetUtil;

public class NetIdePipelineFactory implements ChannelPipelineFactory {
	public ChannelPipeline getPipeline() throws Exception {
	     // Create and configure a new pipeline for a new channel.
	     ChannelPipeline p = Channels.pipeline();
	     p.addLast("frameDecoder", new DelimiterBasedFrameDecoder(80960, Delimiters.lineDelimiter()));
	     p.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
	     p.addLast("myHandler", new BackendChannelHandler());
	     return p;
	   }
}
