# Lambda Notes

* Install `aws` CLI with `pip install awscli`
* make sure u create an IAM user for this work. [I created one called 'lambda' by following this information](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html). The one I created had both console and programmatic access.
* you need to ensure that `aws` has the relevant configuration. It'll work if it detects the environment vars `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN` or it finds `~/.aws` folder. [See this page for details on configuring it](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html).
* you'll need to write a web-based function for AWS. [Here's a blog that seems to get the job done](https://dzone.com/articles/run-code-with-spring-cloud-function-on-aws-lambda). [Here's another one](https://www.infoq.com/news/2017/08/Spring-Cloud-Function-Framework).
* 
