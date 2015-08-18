import eu.netide.core.caos.composition.CompositionSpecification;
import eu.netide.core.caos.composition.CompositionSpecificationLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by timvi on 05.08.2015.
 */
public class DeserializationTest {

    @Test
    public void TestMinimalDeserialization() {
        File testFile = new File("specification/MinimalSpecification.xml");
        Assert.assertNotNull(testFile);
        Assert.assertTrue(testFile.exists(), "Testfile does not exist ('" + testFile.getAbsolutePath() + "')");
        CompositionSpecification cs = null;
        try {
            cs = CompositionSpecificationLoader.Load(Paths.get(testFile.getAbsolutePath()));
        } catch (JAXBException | IOException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(cs, "CompositionSpecification is null");
        Assert.assertNotNull(cs.getModules(), "Modules is null");
        Assert.assertEquals(cs.getModules().size(), 1, "Size of Modules list does not match");
    }

    @Test
    public void TestDeserialization() {
        File testFile = new File("specification/CompositionSpecification.xml");
        Assert.assertNotNull(testFile);
        Assert.assertTrue(testFile.exists(), "Testfile does not exist ('" + testFile.getAbsolutePath() + "')");
        CompositionSpecification cs = null;
        try {
            cs = CompositionSpecificationLoader.Load(Paths.get(testFile.getAbsolutePath()));
        } catch (JAXBException | IOException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(cs, "CompositionSpecification is null");
        Assert.assertNotNull(cs.getModules(), "Modules is null");
        Assert.assertEquals(cs.getModules().size(), 5, "Size of Modules list does not match");
        Assert.assertEquals(cs.getModules().get(3).getCallFilter().getEvents().get(0).value(), "packetIn", "Did not find packetIn event filter");
        Assert.assertEquals(cs.getComposition().size(), 3, "Size of Composition list does not match");
    }
}
