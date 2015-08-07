package eu.netide.core.caos.composition;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by timvi on 25.06.2015.
 */
public class CompositionSpecificationLoader {

    public static CompositionSpecification Load(Path filePath) throws JAXBException, IOException {
        return Load(new String(Files.readAllBytes(filePath)));
    }

    public static CompositionSpecification Load(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(CompositionSpecification.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
        return (CompositionSpecification) unmarshaller.unmarshal(new StringReader(xml));
    }
}
