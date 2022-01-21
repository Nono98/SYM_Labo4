# SYM - Laboratoire 4 - Environnement II (*Capteurs et Bluetooth Low Energy*)

> Auteurs: Adrien Peguiron, Noémie Plancherel, Nicolas Viotti
>
> Date: 30.01.2022

## 1. Capteurs

### 1.2 Question

> Une fois la manipulation effectuée, vous constaterez que les animations de la flèche ne sont  pas fluides, il va y avoir un tremblement plus ou moins important même si le téléphone ne  bouge pas. Veuillez expliquer quelle est la cause la plus probable de ce tremblement et donner une manière (sans forcément l’implémenter) d’y remédier.

Les *sensors* sont assez précis et assez sensibles, donc quelques petits mouvements peuvent provoquer des changements de valeurs et créer un tremblement. On peut expliquer ce phénomène par les raisons suivantes:

1. En fonction de la précision du capteur, il est possible que les valeurs retournées comportent du bruit. Plus la précision du capteur est basse, plus elle comportera du bruit. On peut vérifier ces précisions avec la méthode `onAccurancyChanged` qui retourne le niveau de précision lorsqu'il y a des changements. 
2. Les constantes `SENSOR_DELAY_x` permettent de configurer un délai qui représente l'intervalle de temps auquel les données sont envoyées à l'application (via la méthode `onAccurancyChanged`). Si le délai est trop long, il est plus probable d'avoir des mouvements plus brusques qui pourraient expliquer les tremblements plus ou moins importants.

Pour les deux problèmes précédents, nous pouvons proposer les deux solutions suivantes pour régler le problème de tremblement:

1. On pourrait filtrer les entrées de la méthode `onAccurancyChanged` et n'accepter que les données retournées indiquant une haute précision, comme `SENSOR_STATUS_ACCURANCY_HIGH` ou `SENSOR_STATUS_ACCURANCY_MEIDUM`. Cette méthode peut être limitée car il est possible que pendant un certain temps, on ne reçoive plus de données avec une haute précision, ce qui empêcherait à la boussole de se mettre à jour.
2. Pour y remédier, on peut séléctionner le délai le plus court, `SENSOR_DELAY_FASTEST`, ce qui permettrait de fluidifier le trafic de données et d'en recevoir plus fréquemment.

## 2. Communication *Bluetooth Low Energy*

### 2.2 Questions

> La caractéristique permettant de lire la température retourne la valeur en degrés Celsius, multipliée par 10,sous la forme d’un entier non-signé de 16bits. Quel est l’intérêt de procéder de la sorte? Pourquoi ne pas échanger un nombre à virgule flottante de type float par exemple?



> Le niveau de charge de la pile est à présent indiqué uniquement sur l’écran du périphérique, mais nous souhaiterions que celui-ci puisse informer le smartphone sur son niveau décharge restante. Veuillez spécifier la(les) caractéristique(s) qui composerai(en)t un tel service, mis à disposition par le périphérique etpermettant de communiquerle niveau de batterie restant via Bluetooth Low Energy. Pour chaque caractéristique, vous indiquerez les opérations supportées (lecture, écriture, notification, indication, etc.) ainsi que les données échangées et leur format.