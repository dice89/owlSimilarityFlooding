# OWL SimilarityFlooding
This Project does provide a small wrapper for the original Similarity Flooding Graph Matching Algorithm [1], in order to make it compatabile to the OWL API. 


The Ontology is transformed to a graph as proposed by the YAM++ Ontology Matching System. [2]

[1] Melnik, Sergey, Hector Garcia-Molina, and Erhard Rahm. "Similarity flooding: A versatile graph matching algorithm and its application to schema matching." Data Engineering, 2002. Proceedings. 18th International Conference on. IEEE, 2002.

[2] Ngo, Duy Hoa, Zohra Bellahsene, and Remi Coletta. "YAM++--Results for OAEI 2011." ISWC'11: The 6th International Workshop on Ontology Matching. Vol. 814. 2011.

## Usage

Clone the repository and perform 

``mvn install``

Add the dependency to your Pom, SBT, etc. build file

```xml
<groupId>de.unima.alex</groupId>
<artifactId>owlsimflood</artifactId>
<version>1.0</version> 
```

You can use the example ontologies from the oaei competion to test the usage, a simple jaro winkler based string matcher is included:

```java
        IRI cmt_iri = IRI.create(new File("cmt.owl"));
        OWLOntology cmt = OWLManager.createOWLOntologyManager().loadOntology(cmt_iri);

        IRI conf_iri = IRI.create(new File("Conference.owl"));
        OWLOntology conf = OWLManager.createOWLOntologyManager().loadOntology(conf_iri);

        List<MapPair> initialMapping = TestMatch.createInitialStringMappingJaroWinkler(cmt, conf);
        MapPair[] result = Matcher.structMatch("cmt.owl", "Conference.owl", initialMapping, Match.FORMULA_TTT, Match.FG_PRODUCT);
        Match.dump(result);
```

## Dependencies

- Java 8
- Second String
- Similarity Flooding implementation http://infolab.stanford.edu/~melnik/mm/sfa/
- RDF API
- OWL API
