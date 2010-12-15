package perf;

import java.io.*;
import javax.xml.stream.XMLOutputFactory;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xml.XmlFactory;
import org.codehaus.jackson.xml.XmlMapper;
//import org.codehaus.jackson.smile.*;

import com.ctc.wstx.stax.WstxOutputFactory;
//import com.fasterxml.aalto.stax.OutputFactoryImpl;

public final class TestSerPerf
{
    /*
    /**********************************************************
    /* Actual test
    /**********************************************************
     */

    private final int REPS;

    private TestSerPerf() throws Exception
    {
        // Let's try to guestimate suitable size...
        REPS = 20000;
    }

    private MediaItem buildItem()
    {
        MediaItem.Content content = new MediaItem.Content();
        content.setPlayer(MediaItem.Content.Player.JAVA);
        content.setUri("http://javaone.com/keynote.mpg");
        content.setTitle("Javaone Keynote");
        content.setWidth(640);
        content.setHeight(480);
        content.setFormat("video/mpeg4");
        content.setDuration(18000000L);
        content.setSize(58982400L);
        content.setBitrate(262144);
        content.setCopyright("None");
        content.addPerson("Bill Gates");
        content.addPerson("Steve Jobs");

        MediaItem item = new MediaItem(content);

        item.addPhoto(new MediaItem.Photo("http://javaone.com/keynote_large.jpg", "Javaone Keynote", 1024, 768, MediaItem.Photo.Size.LARGE));
        item.addPhoto(new MediaItem.Photo("http://javaone.com/keynote_small.jpg", "Javaone Keynote", 320, 240, MediaItem.Photo.Size.SMALL));

        return item;
    }
    
    public void test()
        throws Exception
    {
        int i = 0;
        int sum = 0;

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        final MediaItem item = buildItem();
        final JsonFactory jsonF =
//            new JsonFactory()
            new org.codehaus.jackson.smile.SmileFactory();
        ;
//        ((SmileFactory) jsonF).configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, false);
            
        final ObjectMapper jsonMapper = new ObjectMapper(jsonF);

//      jsonMapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);

        XMLOutputFactory xmlOut = new WstxOutputFactory(); // Woodstox
//        XMLOutputFactory xmlOut = new OutputFactoryImpl(); // Aalto
        final XmlMapper xmlMapper = new XmlMapper(new XmlFactory(null, null, xmlOut));

        // Verify that we can roundtrip
        {
            byte[] stuff = jsonMapper.writeValueAsBytes(item);
            @SuppressWarnings("unused")
            MediaItem back = jsonMapper.readValue(stuff, 0, stuff.length, MediaItem.class);
            System.out.println("Warmed up: data size is "+stuff.length+" bytes; "+REPS+" reps -> "
                    +((REPS * stuff.length) >> 10)+" kB per iteration");
            System.out.println();
            stuff = xmlMapper.writeValueAsBytes(item);
            System.out.println(" xml size: "+stuff.length+" bytes; uses "+xmlOut.getClass().getName());
        }
        
        while (true) {
//            Thread.sleep(150L);
            ++i;
//            int round = (i % 1);
            int round = 0;

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = (round == 0);

            switch (round) {

            case 0:
                msg = "Serialize, JSON";
                sum += testObjectSer(jsonMapper, item, REPS, result);
                break;

            case 1:
                msg = "Serialize, xml";
                sum += testObjectSer(xmlMapper, item, REPS, result);
                break;

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    protected int testObjectSer(ObjectMapper mapper, Object value, int reps, ByteArrayOutputStream result)
        throws Exception
    {
        for (int i = 0; i < reps; ++i) {
            result.reset();
            mapper.writeValue(result, value);
        }
        return result.size(); // just to get some non-optimizable number
    }

    public static void main(String[] args) throws Exception
    {
        new TestSerPerf().test();
    }
}
