
  # ================================================================
  #  Dockerfile — API Gestion Transactions Bancaires
  #  Stratégie : Image légère utilisant un JAR pré-compilé
  # ================================================================

  # On utilise directement l'image de base légère (JRE) 
  # car le build se fait sur ta machine
  FROM eclipse-temurin:17-jre-alpine

  # Dossier de travail dans le conteneur
  WORKDIR /app

  # Métadonnées professionnelles
  LABEL maintainer="MEZATIO FOUEGAP GERIL <22u2152@univ.cm>"
  LABEL version="1.0.0"
  LABEL description="API Gestion Transactions Bancaires - Image finale"

  # ---- ACTION CRUCIALE ----
  # On copie le JAR qui se trouve dans ton dossier 'target' local
  # (Assure-toi d'avoir fait 'mvn clean package' avant)
  COPY target/*.jar app.jar
  # Port exposé
  EXPOSE 8080

  # Variables d'environnement par défaut
  ENV PORT=8080

  # Optimisations pour environnement conteneurisé
  # -XX:+UseContainerSupport permet au JAR de respecter les limites RAM de Docker
  ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]