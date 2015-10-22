JEX
==========

The JEX is a set of services and tools that run jobs for the Discovery Environment. Communication between the services and tools is done via AMQP. The JEX component consists of the following:

* __condor-launcher__ : Listens for Condor job launch requests, creates a Condor job submission that will launch road-runner with the correct settings, and submits the job to the Condor cluster.

* __road-runner__ : A command-line tool that orchestrates the various steps that comprise a job execution, including file transfers, Docker-related operations, and clean up operations. road-runner broadcasts job status updates over an AMQP topic exchange.
