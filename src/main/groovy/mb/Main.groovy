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
                'maxrecords': [required: false, defaultValue: '100000'],
                'queryparameters': [required: false, defaultValue: ''],
                'dryrun': [required: false, defaultValue: ''],
                'dryrundoccount': [required: false, defaultValue: '200000']
        ]

        ApplicationContext ctx =
            new AnnotationConfigApplicationContext("mb");

        Main main = ctx.getBean(Main.class);

        main.run(defaultArguments, args.toList())

    }

    void run(Map defaultArguments, List<String> arguments) {

        def errorOutputter = { println it }

        if (!argumentsValidatorInteractor.isValid(errorOutputter, defaultArguments, arguments)) {
            return
        }

        Map settingsMap = argumentsMerge.merge(defaultArguments, arguments)

        def outputDirectory = new File(settingsMap.outputdir.toString())


        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            println "Failed to create output directory. Quitting"
            return
        }

        if (settingsMap.dryrun) println "Dry-run mode"

        Long docCount = settingsMap.dryrun ?  settingsMap.dryrundoccount.toLong() : fetchDocumentCount(settingsMap.dah, settingsMap.dahport)

        Long maxRecords = Math.min(settingsMap.maxrecords.toLong(), docCount)

        Long offset = 1

        while (offset < docCount) {

            Long offsetEnd = offset + maxRecords

            String filename = buildOutputFilename(settingsMap, offset, offsetEnd)
            String requestUri = buildAutonomyRequestUri(settingsMap, offsetEnd, offset)

            println "Fetching ${offset} to ${offsetEnd} of ${docCount} to ${filename}"
            println "Request: ${requestUri}"

            File f = new File(filename)

            try {

                if (!f.exists()) {

                    if (!settingsMap.dryrun) {

                        def xml = new URL(requestUri).text
                        f.write(xml)
                        docCount = parseDocumentCount(xml)

                    }

                }


                offset += maxRecords
            } catch (IOException ex) {

                if (f.exists()) {
                    f.delete()
                }
                ex.printStackTrace()
            }
        }


        println "Finished"
    }

    private String buildOutputFilename(Map settingsMap, long start, end) {
        "${settingsMap.outputdir}/${start}-${end}.xml"
    }

    private String buildAutonomyRequestUri(Map settingsMap, end, long start) {
        String requestUri = "${buildIdolQuery(settingsMap.dah, settingsMap.dahport)}action=query&totalresults=true&predict=false&PrintFields=DUPEDREREFERENCE&maxresults=${end}&start=${start}${settingsMap.queryparameters}"
        requestUri
    }

    long fetchDocumentCount(String dah, String port) {

        String uri = buildIdolQuery(dah, port) + "action=query&totalresults=true&predict=false"
        println "Requesting doc count with: ${uri}"

        def xml = new URL(uri).text
        parseDocumentCount(xml)
    }

    long parseDocumentCount(String xml) {

        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        def inputStream = new ByteArrayInputStream(xml.bytes)
        def records = builder.parse(inputStream).documentElement

        def r = XPathAPI.selectNodeList(records, '//*[local-name()=\'totaldbsecs\']/text()')

        r.item(0).data.toLong()
    }


    String buildIdolQuery(String dah, String port) {
        "http://${dah}:${port}/"
    }

}
