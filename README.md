orioai-nuxeo
============

ori-oai plugin for nuxeo

## Utilisation

Dans un template nuxeo :
*  Placer le jar dans le répertoire plugins
*  Placer un fichier de configuration (Cf. di-dessous) dans le répertoire config

### Fichier de configuration

**Attention** : Ce fichier doit obligatoirement se terminer par -config.xml

#### Exemple de fichier

  <?xml version="1.0"?>
  <!-- your config name -->
  <component name="my.domain.esupecm.workflow.wsdlOriOaiWorkflow">

    <!-- internal config to rewrite -->
    <require>org.orioai.esupecm.workflow.wsdlOriOaiWorkflow</require>

    <!-- extension point to configure -->
    <extension target="org.orioai.esupecm.workflow.service.OriOaiWorkflowService" point="wsUrl">

      <configuration>
        <!-- workflow web service URL -->
        <wsUrl>http://orisrv.domain.my:8888/workflow/xfire/OriWorkflowService</wsUrl>
        <!-- mdEditor.from.url -->
        <mdEditorFromUrl>https://orisrv.domain.my/md-editor</mdEditorFromUrl>
        <!-- mdEditor.to.url -->
        <mdEditorToUrl>https://nuxeosrv.univ-rennes1.fr/md-editor</mdEditorToUrl>
      </configuration>

    </extension>

  </component>
