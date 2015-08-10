package eu.netide.core.caos;

import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.caos.composition.*;
import eu.netide.lib.netip.Message;

import javax.xml.bind.JAXBException;
import java.util.concurrent.Semaphore;

/**
 * Created by timvi on 25.06.2015.
 */
public class CompositionManager implements ICompositionManager, IShimMessageListener {

    private IShimManager shimManager;
    private static String previousCompositionSpecificationXml = "";
    private String compositionSpecificationXml = "";
    private CompositionSpecification compositionSpecification = new CompositionSpecification();
    private Semaphore csLock = new Semaphore(1);


    public void Start() {
        System.out.println("CompositionManager started.");
    }

    public void Stop() {
        System.out.println("CompositionManager stopped.");
    }

    @Override
    public void OnShimMessage(Message message) {
        System.out.println("CompositionManager received message from shim: " + new String(message.getPayload()));
        try {
            csLock.acquire();
            ExecutionFlowStatus status = new ExecutionFlowStatus(message);
            for (ExecutionFlowNode efn : compositionSpecification.getComposition()) {
                ExecutionResult result = efn.Execute(status);
                // TODO handle results
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            csLock.release();
        }
    }

    public void setShimManager(IShimManager manager) {
        shimManager = manager;
    }

    public IShimManager getShimManager() {
        return shimManager;
    }

    public String getCompositionSpecificationXml() {
        return compositionSpecificationXml;
    }

    public void setCompositionSpecificationXml(String compositionSpecificationXml) {
        this.compositionSpecificationXml = compositionSpecificationXml;
        try {
            csLock.acquire();
            this.compositionSpecification = CompositionSpecificationLoader.Load(this.compositionSpecificationXml);
            System.out.println("Accepted new CompositionSpecification '" + this.compositionSpecificationXml + "'.");
            // Accepted specification can be used as fallback
            previousCompositionSpecificationXml = this.compositionSpecificationXml;
        } catch (InterruptedException | JAXBException ex) {
            System.out.println("Unable to read new CompositionSpecification, see error above. Provided XML was '" + this.compositionSpecificationXml + "'.");
            System.out.println("Reusing previous specification '" + previousCompositionSpecificationXml + "'.");
        } finally {
            csLock.release();
        }
    }
}
