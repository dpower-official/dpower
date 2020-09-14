package com.mindata.blockchain.socket.distruptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;
import com.mindata.blockchain.ApplicationContextProvider;
import com.mindata.blockchain.socket.distruptor.base.BaseEvent;
import com.mindata.blockchain.socket.handler.server.PbftVoteHandler;

/**
 * @author wuweifeng wrote on 2018/4/20.
 */
public class DisruptorServerHandler implements EventHandler<BaseEvent> {
	
	private Logger logger = LoggerFactory.getLogger(DisruptorServerHandler.class);

    @Override
    public void onEvent(BaseEvent baseEvent, long sequence, boolean endOfBatch) throws Exception {
    	try {
    		ApplicationContextProvider.getBean(DisruptorServerConsumer.class).receive(baseEvent);
		} catch (Exception e) {
			logger.error("Disruptor事件执行异常",e);
		}
    }
}
