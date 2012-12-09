<?php




$jsonurl = "http://localhost/yolo-hipster/phpfiles/yolo/contributions/main/qui_edit.php?url=fr.wikipedia.org&article=Tokyo_ANA_Hotel";
$json = file_get_contents($jsonurl, false);

$obj = json_decode($json);

var_dump($json);

$result = '<div class="contributeurs">
                <h2>' . $json[0] . '</h2>
            <ol>';


$result .= '           <li>Sophiedeziel : <span>10</span></li>
                <li>ialexca : <span>9</span></li>
                <li>prega : <span>7</span></li>
                <li>rlamour : <span>6</span></li>
                <li class="total">total : <span>32</span></li>
            </ol>
            </div>
            <table>
            <tbody>
                <tr>
                    <th scope="row">sophiedeziel</th>
                    <td>31.25%</td>
                </tr>
                <tr>
                    <th scope="row">ialexca</th>
                    <td>28.125%</td>
                </tr>
                <tr>
                    <th scope="row">prega</th>
                    <td>21.875%</td>
                </tr>
                <tr>
                    <th scope="row">rlamour</th>
                    <td>18.75%</td>
                </tr>
            </tbody>
        </table>';
print $result;
?>
