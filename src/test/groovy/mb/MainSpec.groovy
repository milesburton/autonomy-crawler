package mb

import mb.argumentprocessing.ArgumentsMerge
import mb.usecase.ArgumentsValidatorInteractor

import spock.lang.Specification

class MainSpec extends Specification {

    Main main

    def setup() {
        main = new Main()
        main.argumentsMerge = Mock(ArgumentsMerge)
        main.argumentsValidatorInteractor = Mock(ArgumentsValidatorInteractor)
    }


    def 'run invalid args'() {

        given:
        List<String> arguments = ['--username', 'test', '--password', 'test']
        Map defaults = [
                outputfile: 'test',
                bitbucketurl: 'uri',
                bitbucketrepouri: 'resourrce'
        ]

        when:
        main.run(defaults, arguments)

        then:
        1 * main.argumentsValidatorInteractor.isValid({ true }, defaults, arguments) >> false
        0 * _._

    }
}
