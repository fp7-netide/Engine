package eu.netide.core.shimconnectivity;

import eu.netide.core.api.IShimConnector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Created by timvi on 01.07.2015.
 */
public class SocketBasedShimConnector implements IShimConnector {

    private ChannelFuture _channel;
    private EventLoopGroup _bossGroup;
    private EventLoopGroup _workerGroup;

    public void Open(int port) {
        _bossGroup = new NioEventLoopGroup();
        _workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(_bossGroup, _workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ShimSocketHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
        } catch (InterruptedException ex) {
            System.out.println("Exception in 'Open': " + ex.getMessage());
        }
    }

    public void Close() {
        _channel.channel().closeFuture().syncUninterruptibly();
        _workerGroup.shutdownGracefully();
        _bossGroup.shutdownGracefully();
    }
}
