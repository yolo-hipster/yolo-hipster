<?php

$urlcomplet = $_GET["url"];

$urlcomplet = $json = preg_replace('/http:\/\//', '', $urlcomplet);

$split = explode("/wiki/", $urlcomplet);

$url = $split[0];
$article = $split[1];


$jsonurl = "http://localhost:8888/yolo-hipster/phpfiles/yolo/contributions/main/qui_edit.php?url=" . $url . "&article=" . $article;



$json = file_get_contents($jsonurl, true);
$json = preg_replace('/\x{EF}\x{BB}\x{BF} /', '', $json);
$obj = json_decode($json);



$result = '<h1>Contributeurs - Page: '.$article.'Home</h1>
            <div id="content">
            
<div class="contributeurs">
                <h2>Contributeurs</h2>
            <ol>';
$nombre = $obj->Quantite;

foreach ($obj->Edits as $user) {
    $liste .= '<li class="user' . $user->Id . '">' . $user->UserName . ' : <span>' . $user->Number . '</span></li>';
    $table .= '<tr>
                    <th scope="row" >' . $user->UserName . '</th>
                    <td>' . ($user->Number / $nombre * 100) . '%</td>
                </tr>';
}

$result .= $liste;


$result .= ' </ol>
            </div>
            <table><tbody>';

$result .= $table;

$result .= '</tbody>
        </table>
        </div>
            <div id="holder"></div>';
print $result;
?>
