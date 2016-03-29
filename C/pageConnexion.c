#include "pagePrincipale.h"

GtkWidget *fenetre_connexion = NULL;
GtkBuilder *builder = NULL;
GError *error = NULL;
gchar *filename = NULL;


typedef int SOCKET;
SOCKET sock;
char user[255];

int connexion(GtkWidget * p_wid, gpointer p_data){

	GtkEntry* entry = (GtkEntry*)p_data;
	//puts(gtk_entry_get_text(entry)); //Recuperation du texte de l'area text

	//Initialisation des valeur
	char buffer[1024];
	char messageEnvoye[255];
	char connexionReussi[255];
    char fin[8], name[50];
    // strcpy(fin,"\0\r\n");
    // strcpy(messageEnvoye,"CONNEX/");
    sprintf(name, "%s",  gtk_entry_get_text(entry));
    sprintf(messageEnvoye, "CONNEX/%s/\n", name);

    if(send(sock, messageEnvoye, strlen(messageEnvoye), 0) < 0)
    {
        perror("send(pseudo)");
        return;
    }

    puts("Connected\n");
}



/* callback */
static void destroy( GtkWidget *widget, gpointer   data ){
    gtk_main_quit ();
}
 
void startPageConnexion(){
    gtk_init(NULL,NULL);
    builder = gtk_builder_new();
    filename =  g_build_filename ("pageDeCo.glade", NULL);

    gtk_builder_add_from_file (builder, filename, &error);
    g_free (filename);
    if (error){
        gint code = error->code;
        g_printerr("%s\n", error->message);
        g_error_free (error);
        return;
    }

    fenetre_connexion = GTK_WIDGET(gtk_builder_get_object (builder, "window1"));

    g_signal_connect (gtk_builder_get_object (builder, "connexion"), "clicked", G_CALLBACK (connexion),(gpointer)(gtk_builder_get_object(builder, "userText")));
    g_signal_connect (fenetre_connexion, "destroy", G_CALLBACK (destroy), NULL);

    gtk_widget_show_all (fenetre_connexion);
    gtk_main();

    printf("Fin de la fonction startPageConnexion\n");
}

void destroyPageConnexion(){
	printf("Destruction de la fenetre\n");
	gtk_widget_destroy(fenetre_connexion);
	printf("Destroy reussi\n");
}