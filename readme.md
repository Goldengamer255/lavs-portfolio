# Mainframe Interface: Project 2 Starter

Welcome to the second project for SOFTENG 306.

The project specification is available [on Canvas](https://canvas.auckland.ac.nz/courses/142477/pages/project-2-overview?module_item_id=3007942). The four deliverables for this project are:

- [Proposal](https://canvas.auckland.ac.nz/courses/142477/assignments/514280)
- [Presentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514279)
- [Code](https://canvas.auckland.ac.nz/courses/142477/assignments/514277)
- [Documentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514278)

## Important

Update this readme file to contain the names of each team member and your GitHub login.

- Jim Zhou - FridgeProtector1
- Hargun Singh - Hargun2005
- Rohit Balaji - RohitB2005
- _Replace this text: Team Member Name 4 - GitHub Login_

## Running LAVS

Requirements: JDK 17 or newer, Maven, and the numeric mainframe organisation ID
assigned to the team.

From the project root, run:

```bash
export LAVS_ORGANISATION_ID="<assigned numeric organisation ID>"
mvn compile
mvn javafx:run
```

The mainframe URL defaults to the course server. It can be overridden with the
`LAVS_BASE_URL` environment variable. The organisation ID is assigned to newly
created customers and is deliberately kept out of the form and source code.

## Initial Repository Code

This repository initially contains the code for the mainframe interface and the unit tests. The following code is supplied:

- [mainframe](src/main/java/mainframe/client/): the interfaces for sending requests to and receiving responses from the mainframe. You will need to use the [Connection](src/main/java/mainframe/client/Connection.java) interface in your code. We have provided an implementation that connects to a web server in [HttpConnection](src/main/java/mainframe/client/HttpConnection.java).
- [tests](src/test/java/mainframe/client/): the unit tests. Most of these connect to the web server, but there is an [example unit test](src/test/java/mainframe/client/SimulateListCustomers.java) for connecting to a stub.
