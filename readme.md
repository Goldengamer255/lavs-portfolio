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
- Tanush Panuganti - Goldengamer255

## Building, Testing, and Running LAVS on Windows

Requirements: JDK 17 or newer and Maven.

Create a local configuration file only if you want to run the live-mainframe tests.
Enter the team-issued username and password in `.env`; this file is ignored by Git.

### Windows PowerShell

Confirm that Java and Maven are available, then copy the configuration template:

```powershell
java -version
mvn -version
Copy-Item .env.example .env
notepad .env
```

Optionally enter the team-issued credentials, then build, test, and run the application:

```powershell
mvn clean compile
mvn test
mvn javafx:run
```

### Windows Command Prompt

```bat
java -version
mvn -version
copy .env.example .env
notepad .env
mvn compile
mvn test
mvn javafx:run
```

Plain `mvn test` runs the LAVS tests and writes the coverage report to `target/site/jacoco/index.html`.
The supplied live-server tests are skipped by default, because their result depends on shared server data, permissions and availability.
To run them as well, set up `.env` and use `mvn test -Dlive`.

The mainframe URL defaults to the course server. It can be overridden with the
`LAVS_BASE_URL` environment variable. New customers use the logged-in user's
organisation by default. A Root user can override this by setting
`LAVS_ORGANISATION_ID` to another organisation's numeric ID.

## Initial Repository Code

This repository initially contains the code for the mainframe interface and the unit tests. The following code is supplied:

- [mainframe](src/main/java/mainframe/client/): the interfaces for sending requests to and receiving responses from the mainframe. You will need to use the [Connection](src/main/java/mainframe/client/Connection.java) interface in your code. We have provided an implementation that connects to a web server in [HttpConnection](src/main/java/mainframe/client/HttpConnection.java).
- [tests](src/test/java/mainframe/client/): the unit tests. Most of these connect to the web server, but there is an [example unit test](src/test/java/mainframe/client/SimulateListCustomers.java) for connecting to a stub.
