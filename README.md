# Spreadsheet component for Vaadin Flow, with modifications by Vaadin Component Factory

A modified spreadsheet component for [Vaadin Flow](https://github.com/vaadin/flow).

This is a modification of: https://github.com/vaadin/flow-components/vaadin-spreadsheet-flow-parent/
which is a port of https://github.com/vaadin/spreadsheet

The VCF version of Vaadin Spreadsheet currently provides pre-release features:

* Color support for custom formatting (the Excel base color names, Excel indexed
  colors and the POI HSSFColor predefined constants).
* Major performance improvement when using autofilter/SpreadsheetFilterTable on large
  data sets.

## Version history

Version 1.0 built on flow-components 24.7.1.

## Using the component in a Flow application

To use the component in an application using maven,
add the following dependency to your `pom.xml`:
```
<dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>vcf-spreadsheet-flow</artifactId>
    <version>${component.version}</version>
</dependency>
```

## Build instructions

This component has been repackaged from a multi-module project to a single-module project.
Consequently, the build is no longer fully automated.

Short version:
* Clean the project using `mvn clean`
* Build the client side of the component: `cd client` and `mvn package` - the client side build
  process copies the built javascript file to the server-side resource directory
* Finally, build the server-side component: `mvn -DskipTests package`

To run integration tets, change to the `integration-tests` directory and run `mvn verify`

## License

This component is distributed under [Vaadin Commercial License and Service Terms](https://vaadin.com/commercial-license-and-service-terms).

To purchase a license, visit http://vaadin.com/pricing
