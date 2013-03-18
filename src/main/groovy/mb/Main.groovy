package mb

import com.sun.org.apache.xpath.internal.XPathAPI
import mb.argumentprocessing.ArgumentsMerge
import mb.usecase.ArgumentsValidatorInteractor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component

import javax.xml.parsers.DocumentBuilderFactory

@Component
class Main {

    @Autowired
    ArgumentsValidatorInteractor argumentsValidatorInteractor

    @Autowired
    ArgumentsMerge argumentsMerge

    static void main(String[] args) {

        def defaultArguments = [
                'dah': [required: false, defaultValue: 'localhost'],
                'dahport': [required: false, defaultValue: '16554'],
                'outputdir': [required: false, defaultValue: 'queries'],
                'maxrecords': [required: false, defaultValue: '100000']
        ]

        ApplicationContext ctx =
            new AnnotationConfigApplicationContext("mb");

        Main main = ctx.getBean(Main.class);

        main.run(defaultArguments, args.toList())

    }

    void run(Map defaultArguments, List<String> arguments) {

        def errorOutputter = { println it }

        if (argumentsValidatorInteractor.isValid(errorOutputter, defaultArguments, arguments)) {

            Map settingsMap = argumentsMerge.merge(defaultArguments, arguments)

            println arguments
            println settingsMap

            def outputDirectory = new File(settingsMap.outputdir.toString())


            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                println "Failed to create output directory. Quitting"
                return
            }

            Long docCount = fetchDocumentCount(settingsMap.dah, settingsMap.dahport)

            Long maxRecords = Math.min(settingsMap.maxrecords.toLong(), docCount)

            Long start = 1

            while (start < docCount) {

                def end = start + maxRecords

                def filename = "${settingsMap.outputdir}/${start}-${end}.xml"
                String requestUri = "${buildIdolQuery(settingsMap.dah, settingsMap.dahport)}action=query&PrintFields=DUPEDREREFERENCE&maxresults=${end}&start=${start}"

                println "Fetching ${start} to ${end} of ${docCount} to ${filename}"
                println "Request: ${requestUri}"


                try {
                    File f = new File(filename)

                    if (!f.exists()) {

                        println "Fetch: ${requestUri}"

                        f.write(new URL(requestUri).text)
                    }


                    start += maxRecords
                } catch (IOException ex) {

                    ex.printStackTrace()
                }
            }
        }

        println "Finished"
    }

    long fetchDocumentCount(String dah, String port) {

        String uri = buildIdolQuery(dah, port) + "action=query&totalresults=true"


        def foo = new XmlSlurper().parse(uri).declareNamespace(autn: 'http://www.autonomy.com')

        println foo.responsedata.'autn:totaldbdocs'

        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        def inputStream = new ByteArrayInputStream(new URL(uri).text.bytes)
        def records = builder.parse(inputStream).documentElement

        def r = XPathAPI.selectNodeList(records, '//*[local-name()=\'totaldbsecs\']/text()')

        r.item(0).data.toLong()
    }

    String buildIdolQuery(String dah, String port) {
        "http://${dah}:${port}/"
    }

}
