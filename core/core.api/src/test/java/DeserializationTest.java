import eu.netide.core.api.composition.CompositionSpecification;
import eu.netide.core.api.composition.CompositionSpecificationLoader;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Created by timvi on 05.08.2015.
 */
public class DeserializationTest {

    @Test
    public void TestMinimalDeserialization() {
        Path testFile = FileSystems.getDefault().getPath("C:\\Users\\timvi\\Source\\Repos\\Masterarbeit\\Engine\\core\\specification\\MinimalSpecification.xml");
        Assert.assertNotNull(testFile);
        Assert.assertTrue(testFile.toFile().exists(), "Testfile does not exist");
        CompositionSpecification cs = null;
        try {
            cs = CompositionSpecificationLoader.Load(testFile);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(cs, "CompositionSpecification is null");
        Assert.assertNotNull(cs.getModules(), "Modules is null");
        Assert.assertEquals(cs.getModules().size(), 1, "Size of Modules list does not match");
    }

    @Test
    public void TestDeserialization() {
        Path testFile = FileSystems.getDefault().getPath("C:\\Users\\timvi\\Source\\Repos\\Masterarbeit\\Engine\\core\\specification\\CompositionSpecification.xml");
        Assert.assertNotNull(testFile);
        Assert.assertTrue(testFile.toFile().exists(), "Testfile does not exist");
        CompositionSpecification cs = null;
        try {
            cs = CompositionSpecificationLoader.Load(testFile);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(cs, "CompositionSpecification is null");
        Assert.assertNotNull(cs.getModules(), "Modules is null");
        Assert.assertEquals(cs.getModules().size(), 5, "Size of Modules list does not match");
        Assert.assertEquals(cs.getModules().get(3).getCallFilter().getEvents().get(0).value(), "packetIn", "Did not find packetIn event filter");
        Assert.assertEquals(cs.getComposition().size(), 3, "Size of Composition list does not match");
    }
}
